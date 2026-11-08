package br.com.jaquesprojetos.blog.backend

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DatabaseAdminAuthStoreTest {
    @Test
    fun `timestamptz binding uses the supported UTC OffsetDateTime type`() {
        val instant = Instant.parse("2026-11-08T03:20:00Z")

        val parameter = postgresTimestamptz(instant)

        assertEquals(ZoneOffset.UTC, parameter.offset)
        assertEquals(instant, parameter.toInstant())
    }

    @Test
    fun `postgres persists and revokes oidc transactions and admin sessions when configured`() {
        val url = System.getenv("BLOG_TEST_DATABASE_URL")
        if (url == null) {
            assumeTrue("BLOG_TEST_DATABASE_URL is not set; PostgreSQL integration test skipped", false)
            return
        }
        require(url.startsWith("jdbc:postgresql://127.0.0.1:") && url.substringAfterLast('/') == "ljblog_auth_test") {
            "BLOG_TEST_DATABASE_URL must point to the local ljblog_auth_test database"
        }
        val database = Database(
            mapOf(
                "DATABASE_URL" to url,
                "DATABASE_USER" to (System.getenv("BLOG_TEST_DATABASE_USER") ?: "blog_test"),
                "DATABASE_PASSWORD" to (System.getenv("BLOG_TEST_DATABASE_PASSWORD") ?: "blog_test"),
                "DATABASE_POOL_SIZE" to "2",
            )
        )
        try {
            val store = database as AdminAuthStore
            val stateHash = digest("integration-state-${System.nanoTime()}")
            val nonceHash = digest("integration-nonce-${System.nanoTime()}")
            val verifier = "test-verifier-value"
            store.insertOidcTransaction(stateHash, nonceHash, verifier)

            val transaction = store.consumeOidcTransaction(stateHash)
            assertContentEquals(nonceHash, transaction?.nonceHash)
            assertEquals(verifier, transaction?.verifier)
            assertNull(store.consumeOidcTransaction(stateHash), "OIDC state must be single-use")

            val tokenHash = digest("integration-session-${System.nanoTime()}")
            store.insertAdminSession(
                tokenHash,
                "test-subject",
                setOf("blog-admin"),
                Instant.now().plusSeconds(300),
            )
            assertEquals("test-subject", store.findAdminSession(tokenHash)?.subject)
            store.revokeAdminSession(tokenHash)
            assertNull(store.findAdminSession(tokenHash), "revoked session must no longer be accepted")
        } finally {
            database.close()
        }
    }

    private fun digest(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
}
