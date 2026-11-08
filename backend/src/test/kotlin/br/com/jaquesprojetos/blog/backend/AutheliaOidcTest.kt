package br.com.jaquesprojetos.blog.backend

import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutheliaOidcTest {
    @Test
    fun `basic auth form-encodes both client credentials before base64`() {
        val encoded = basicAuthCredentials("blog client", "a:b + c/=")
        val decoded = Base64.getDecoder().decode(encoded).toString(StandardCharsets.UTF_8)

        assertEquals("blog+client:a%3Ab+%2B+c%2F%3D", decoded)
    }

    @Test
    fun `token request uses basic auth without a duplicate client id body parameter`() {
        val form = authorizationCodeTokenForm(
            code = "auth code+/=",
            redirectUri = "https://example.test/callback",
            verifier = "pkce verifier",
        )

        assertEquals(
            "grant_type=authorization_code&code=auth+code%2B%2F%3D&redirect_uri=https%3A%2F%2Fexample.test%2Fcallback&code_verifier=pkce+verifier",
            form,
        )
        assertFalse(form.contains("client_id"))
    }

    @Test
    fun `userinfo request sends the access token as bearer authorization`() {
        val request = userInfoRequest("https://auth.example.test/api/oidc/userinfo", "test-access-token")

        assertEquals("GET", request.method())
        assertEquals("Bearer test-access-token", request.headers().firstValue("Authorization").orElse(null))
        assertEquals("application/json", request.headers().firstValue("Accept").orElse(null))
    }

    @Test
    fun `userinfo groups are read from the JSON array`() {
        val userInfo = parseUserInfoClaims(
            """{"sub":"leo-subject","groups":["lldap_admin","blog-admin",42]}""",
            "leo-subject",
        )

        assertEquals("leo-subject", userInfo.subject)
        assertEquals(setOf("lldap_admin", "blog-admin"), userInfo.groups)
        assertTrue("blog-admin" in userInfo.groups)
    }

    @Test
    fun `userinfo rejects a subject that differs from the verified id token`() {
        val failure = assertFailsWith<OidcCallbackFailure> {
            parseUserInfoClaims("""{"sub":"someone-else","groups":["blog-admin"]}""", "leo-subject")
        }

        assertEquals("userinfo_subject_mismatch", failure.stage)
    }

    @Test
    fun `userinfo requires a subject and a valid object response`() {
        val missingSubject = assertFailsWith<OidcCallbackFailure> {
            parseUserInfoClaims("""{"groups":["blog-admin"]}""", "leo-subject")
        }
        val invalidJson = assertFailsWith<OidcCallbackFailure> {
            parseUserInfoClaims("not-json", "leo-subject")
        }

        assertEquals("userinfo_subject_missing", missingSubject.stage)
        assertEquals("userinfo_parse", invalidJson.stage)
    }

    @Test
    fun `admin access requires the configured group`() {
        requireAdminGroup(setOf("lldap_admin", "blog-admin"), "blog-admin")

        val failure = assertFailsWith<OidcCallbackFailure> {
            requireAdminGroup(setOf("lldap_admin"), "blog-admin")
        }
        assertEquals("admin_group", failure.stage)
    }
}
