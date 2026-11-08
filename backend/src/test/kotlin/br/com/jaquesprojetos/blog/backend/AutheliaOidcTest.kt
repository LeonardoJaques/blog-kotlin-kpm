package br.com.jaquesprojetos.blog.backend

import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals

class AutheliaOidcTest {
    @Test
    fun `basic auth form-encodes both client credentials before base64`() {
        val encoded = basicAuthCredentials("blog client", "a:b + c/=")
        val decoded = Base64.getDecoder().decode(encoded).toString(StandardCharsets.UTF_8)

        assertEquals("blog+client:a%3Ab+%2B+c%2F%3D", decoded)
    }
}
