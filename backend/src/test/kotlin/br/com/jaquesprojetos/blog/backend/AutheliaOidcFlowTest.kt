package br.com.jaquesprojetos.blog.backend

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AutheliaOidcFlowTest {
    @Test
    fun `full authorization code flow verifies provider and creates admin session`() = withFlow { flow ->
        val (state, nonce) = flow.begin()
        val session = flow.auth.complete(state, "one-time-code")

        assertNotNull(session)
        assertEquals("user-123", session.subject)
        assertEquals("user-123", flow.auth.session(session.token)?.subject)
        assertTrue(flow.provider.tokenWasValidated)
        assertTrue(flow.provider.userInfoWasAuthorized)
        assertEquals(1, flow.store.sessions.size)
    }

    @Test
    fun `http callback completes login then issues protected cookies and redirects`() = withFlow { flow ->
        val (state) = flow.begin()

        testApplication {
            environment { config = MapApplicationConfig("ktor.application.modules.size" to "0") }
            application {
                install(ContentNegotiation) { json() }
                routing {
                    route("/api/v1") {
                        adminOidcCallback(flow.auth, secureCookies = true, webBaseUrl = "https://blog.example.test/ljblog")
                    }
                }
            }
            val callbackClient = createClient { followRedirects = false }
            val encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8)
            val response = callbackClient.get("/api/v1/admin/callback?state=$encodedState&code=one-time-code")

            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("https://blog.example.test/ljblog/admin", response.headers[HttpHeaders.Location])
            val cookies = response.headers.getAll(HttpHeaders.SetCookie).orEmpty()
            val sessionCookie = cookies.first { it.startsWith("admin_session=") }
            val csrfCookie = cookies.first { it.startsWith("admin_csrf=") }
            assertTrue("Secure" in sessionCookie)
            assertTrue("HttpOnly" in sessionCookie)
            assertTrue("SameSite=Lax" in sessionCookie)
            assertTrue("Path=/" in sessionCookie)
            assertTrue("Secure" in csrfCookie)
            assertTrue("HttpOnly" !in csrfCookie)
            assertTrue("SameSite=Lax" in csrfCookie)
            assertTrue("Path=/" in csrfCookie)
        }
    }

    @Test
    fun `http callback fails closed without setting any cookies`() = withFlow { flow ->
        val (state) = flow.begin()
        flow.provider.groups = listOf("reader")

        testApplication {
            environment { config = MapApplicationConfig("ktor.application.modules.size" to "0") }
            application {
                install(ContentNegotiation) { json() }
                routing {
                    route("/api/v1") {
                        adminOidcCallback(flow.auth, secureCookies = true, webBaseUrl = "https://blog.example.test/ljblog")
                    }
                }
            }
            val encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8)
            val response = client.get("/api/v1/admin/callback?state=$encodedState&code=one-time-code")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertTrue("oidc_login_failed" in response.bodyAsText())
            assertTrue(response.headers.getAll(HttpHeaders.SetCookie).isNullOrEmpty())
        }
        assertTrue(flow.store.sessions.isEmpty())
    }

    @Test
    fun `authorization state can only be consumed once`() = withFlow { flow ->
        val (state) = flow.begin()
        val session = flow.auth.complete(state, "one-time-code")
        assertNotNull(session)

        val failure = assertFailsWith<OidcCallbackFailure> {
            flow.auth.complete(state, "replayed-code")
        }
        assertEquals("state", failure.stage)
        assertEquals(1, flow.store.sessions.size)
    }

    @Test
    fun `invalid id token signature is rejected before userinfo`() = withFlow { flow ->
        val (state) = flow.begin()
        flow.provider.signingKey = RSAKeyGenerator(2048).keyID("attacker-key").generate()

        val failure = assertFailsWith<OidcCallbackFailure> {
            flow.auth.complete(state, "one-time-code")
        }

        assertEquals("id_token_verification", failure.stage)
        assertNull(flow.provider.lastUserInfoAuthorization)
        assertTrue(flow.store.sessions.isEmpty())
    }

    @Test
    fun `nonce mismatch is rejected`() = withFlow { flow ->
        val (state, nonce) = flow.begin()
        flow.provider.tokenNonce = "$nonce-tampered"

        val failure = assertFailsWith<OidcCallbackFailure> {
            flow.auth.complete(state, "one-time-code")
        }

        assertEquals("id_token_verification", failure.stage)
        assertTrue(flow.store.sessions.isEmpty())
    }

    @Test
    fun `id token issuer and audience must match the registered client`() = withFlow { flow ->
        val (state) = flow.begin()
        flow.provider.tokenIssuer = "http://127.0.0.1:${flow.provider.port}/other-issuer"
        flow.provider.tokenAudience = "different-client"

        val failure = assertFailsWith<OidcCallbackFailure> {
            flow.auth.complete(state, "one-time-code")
        }

        assertEquals("id_token_verification", failure.stage)
        assertTrue(flow.store.sessions.isEmpty())
    }

    @Test
    fun `expired id token is rejected`() = withFlow { flow ->
        val (state) = flow.begin()
        flow.provider.tokenLifetimeSeconds = -60

        val failure = assertFailsWith<OidcCallbackFailure> {
            flow.auth.complete(state, "one-time-code")
        }

        assertEquals("id_token_verification", failure.stage)
        assertTrue(flow.store.sessions.isEmpty())
    }

    @Test
    fun `userinfo subject must match the verified id token`() = withFlow { flow ->
        val (state) = flow.begin()
        flow.provider.userInfoSubject = "different-user"

        val failure = assertFailsWith<OidcCallbackFailure> {
            flow.auth.complete(state, "one-time-code")
        }

        assertEquals("userinfo_subject_mismatch", failure.stage)
        assertTrue(flow.store.sessions.isEmpty())
    }

    @Test
    fun `user without blog admin group cannot receive a session`() = withFlow { flow ->
        val (state) = flow.begin()
        flow.provider.groups = listOf("lldap_admin")

        val failure = assertFailsWith<OidcCallbackFailure> {
            flow.auth.complete(state, "one-time-code")
        }

        assertEquals("admin_group", failure.stage)
        assertTrue(flow.store.sessions.isEmpty())
    }

    @Test
    fun `invalid pkce verifier is rejected by token endpoint`() = withFlow { flow ->
        val (state) = flow.begin()
        flow.store.changeVerifierForNextConsume = "not-the-original-verifier"

        val failure = assertFailsWith<OidcCallbackFailure> {
            flow.auth.complete(state, "one-time-code")
        }

        assertEquals("token_exchange", failure.stage)
        assertTrue(flow.store.sessions.isEmpty())
    }

    @Test
    fun `session revocation invalidates the issued session`() = withFlow { flow ->
        val (state) = flow.begin()
        val session = assertNotNull(flow.auth.complete(state, "one-time-code"))
        assertNotNull(flow.auth.session(session.token))

        flow.auth.revoke(session.token)

        assertNull(flow.auth.session(session.token))
    }

    private fun withFlow(block: (TestFlow) -> Unit) {
        val provider = MockOidcProvider()
        try {
            val store = MemoryAdminAuthStore()
            val env = mapOf(
                "OIDC_ISSUER" to provider.issuer,
                "OIDC_CLIENT_ID" to CLIENT_ID,
                "OIDC_CLIENT_SECRET" to CLIENT_SECRET,
                "OIDC_REDIRECT_URI" to "https://blog.example.test/ljblog/api/v1/admin/callback",
                "OIDC_ADMIN_GROUP" to "blog-admin",
            )
            block(TestFlow(AutheliaOidc(store, env), store, provider))
        } finally {
            provider.close()
        }
    }

    private data class TestFlow(
        val auth: AutheliaOidc,
        val store: MemoryAdminAuthStore,
        val provider: MockOidcProvider,
    ) {
        fun begin(): Pair<String, String> {
            val parameters = query(URI(auth.authorizationUrl()).rawQuery)
            val state = parameters.getValue("state")
            val nonce = parameters.getValue("nonce")
            provider.expectedCodeChallenge = parameters.getValue("code_challenge")
            provider.expectedNonce = nonce
            return state to nonce
        }
    }

    private class MemoryAdminAuthStore : AdminAuthStore {
        private val transactions = ConcurrentHashMap<String, OidcTransactionRecord>()
        val sessions = ConcurrentHashMap<String, TestSession>()
        @Volatile var changeVerifierForNextConsume: String? = null

        override fun insertOidcTransaction(stateHash: ByteArray, nonceHash: ByteArray, verifier: String) {
            check(transactions.putIfAbsent(key(stateHash), OidcTransactionRecord(nonceHash, verifier)) == null)
        }

        override fun consumeOidcTransaction(stateHash: ByteArray): OidcTransactionRecord? {
            val row = transactions.remove(key(stateHash)) ?: return null
            val verifier = changeVerifierForNextConsume.also { changeVerifierForNextConsume = null } ?: row.verifier
            return row.copy(verifier = verifier)
        }

        override fun insertAdminSession(tokenHash: ByteArray, subject: String, groups: Set<String>, expiresAt: Instant) {
            sessions[key(tokenHash)] = TestSession(subject, groups, expiresAt, revoked = false)
        }

        override fun findAdminSession(tokenHash: ByteArray): StoredAdminSession? {
            val session = sessions[key(tokenHash)] ?: return null
            if (session.revoked || !session.expiresAt.isAfter(Instant.now())) return null
            return StoredAdminSession(session.subject, session.groups)
        }

        override fun revokeAdminSession(tokenHash: ByteArray) {
            sessions.computeIfPresent(key(tokenHash)) { _, session -> session.copy(revoked = true) }
        }

        private fun key(value: ByteArray) = Base64.getEncoder().encodeToString(value)
        data class TestSession(val subject: String, val groups: Set<String>, val expiresAt: Instant, val revoked: Boolean)
    }

    private class MockOidcProvider : AutoCloseable {
        private val jwksSigningKey = RSAKeyGenerator(2048).keyID("mock-key").generate()
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val issuer = "http://127.0.0.1:${server.address.port}/issuer"
        val port get() = server.address.port
        var expectedNonce: String? = null
        var expectedCodeChallenge: String? = null
        var tokenNonce: String? = null
        var tokenIssuer: String? = null
        var tokenAudience: String? = null
        var tokenLifetimeSeconds = 300L
        var signingKey: RSAKey = jwksSigningKey
        var userInfoSubject = "user-123"
        var groups = listOf("lldap_admin", "blog-admin")
        var tokenWasValidated = false
        var userInfoWasAuthorized = false
        var lastUserInfoAuthorization: String? = null
            private set
        init {
            server.createContext("/") { exchange -> handle(exchange) }
            server.start()
        }

        override fun close() = server.stop(0)

        private fun handle(exchange: HttpExchange) {
            try {
                when (exchange.requestURI.path) {
                    "/issuer/.well-known/openid-configuration" -> respond(exchange, 200, buildJsonObject {
                        put("issuer", issuer)
                        put("authorization_endpoint", "$issuer/authorize")
                        put("token_endpoint", "$issuer/token")
                        put("jwks_uri", "$issuer/jwks")
                        put("userinfo_endpoint", "$issuer/userinfo")
                    }.toString())
                    "/issuer/jwks" -> respond(exchange, 200, JWKSet(jwksSigningKey.toPublicJWK()).toString())
                    "/issuer/token" -> token(exchange)
                    "/issuer/userinfo" -> userInfo(exchange)
                    else -> respond(exchange, 404, "{}")
                }
            } catch (_: Exception) {
                runCatching { respond(exchange, 500, "{}") }
            }
        }

        private fun token(exchange: HttpExchange) {
            val basic = Base64.getDecoder().decode(exchange.requestHeaders.getFirst("Authorization").orEmpty().removePrefix("Basic "))
                .toString(StandardCharsets.UTF_8)
            val form = query(exchange.requestBody.use { it.readBytes().toString(StandardCharsets.UTF_8) })
            val verifier = form["code_verifier"].orEmpty()
            val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(StandardCharsets.US_ASCII))
            )
            if (basic != "$CLIENT_ID:$CLIENT_SECRET" || form["grant_type"] != "authorization_code" ||
                form["code"] != "one-time-code" || form["redirect_uri"] != REDIRECT_URI ||
                challenge != expectedCodeChallenge
            ) {
                respond(exchange, 400, """{"error":"invalid_grant"}""")
                return
            }
            tokenWasValidated = true
            val now = Instant.now()
            val claims = JWTClaimsSet.Builder()
                .issuer(tokenIssuer ?: issuer)
                .subject("user-123")
                .audience(tokenAudience ?: CLIENT_ID)
                .issueTime(Date.from(now.minusSeconds(1)))
                .expirationTime(Date.from(now.plusSeconds(tokenLifetimeSeconds)))
                .claim("nonce", tokenNonce ?: expectedNonce)
                .build()
            val jwt = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.keyID).build(), claims)
            jwt.sign(RSASSASigner(signingKey.toPrivateKey()))
            val response = buildJsonObject {
                put("token_type", "Bearer")
                put("access_token", "mock-access-token")
                put("expires_in", 300)
                put("id_token", jwt.serialize())
            }
            respond(exchange, 200, response.toString())
        }

        private fun userInfo(exchange: HttpExchange) {
            lastUserInfoAuthorization = exchange.requestHeaders.getFirst("Authorization")
            if (lastUserInfoAuthorization != "Bearer mock-access-token") {
                respond(exchange, 401, "{}")
                return
            }
            userInfoWasAuthorized = true
            val response = buildJsonObject {
                put("sub", userInfoSubject)
                put("groups", kotlinx.serialization.json.buildJsonArray { groups.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
            }
            respond(exchange, 200, response.toString())
        }

        private fun respond(exchange: HttpExchange, status: Int, body: String) {
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }

    companion object {
        private const val CLIENT_ID = "test-client"
        private const val CLIENT_SECRET = "test-secret"
        private const val REDIRECT_URI = "https://blog.example.test/ljblog/api/v1/admin/callback"

        private fun query(rawQuery: String): Map<String, String> = rawQuery.split('&').associate { pair ->
            val parts = pair.split('=', limit = 2)
            URLDecoder.decode(parts[0], StandardCharsets.UTF_8) to
                URLDecoder.decode(parts.getOrElse(1) { "" }, StandardCharsets.UTF_8)
        }
    }
}
