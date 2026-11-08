package br.com.jaquesprojetos.blog.backend

import java.nio.charset.StandardCharsets
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Executable smoke test for an isolated PostgreSQL database. */
object AdminAuthStorePostgresSmoke {
    @JvmStatic
    fun main(args: Array<String>) {
        val url = System.getenv("BLOG_TEST_DATABASE_URL")
        require(url?.startsWith("jdbc:postgresql://ljblog-auth-test-pg:5432/ljblog_auth_test") == true) {
            "This smoke test only runs against the isolated ljblog-auth-test-pg database"
        }

        val database = Database(
            mapOf(
                "DATABASE_URL" to url,
                "DATABASE_USER" to "blog_test",
                "DATABASE_PASSWORD" to "blog_test",
                "DATABASE_POOL_SIZE" to "2",
            )
        )
        try {
            val stateHash = digest("state-${UUID.randomUUID()}")
            val nonceHash = digest("nonce-${UUID.randomUUID()}")
            val verifier = "pkce-verifier-for-postgres-smoke-test"
            database.insertOidcTransaction(stateHash, nonceHash, verifier)
            val transaction = database.consumeOidcTransaction(stateHash)
            check(transaction != null && verifier == transaction.verifier && MessageDigest.isEqual(nonceHash, transaction.nonceHash)) {
                "OIDC transaction did not round-trip through PostgreSQL"
            }
            check(database.consumeOidcTransaction(stateHash) == null) { "OIDC state was not single-use" }

            val sessionHash = digest("session-${UUID.randomUUID()}")
            database.insertAdminSession(sessionHash, "test-subject", setOf("blog-admin"), Instant.now().plusSeconds(120))
            val session = database.findAdminSession(sessionHash)
            check(session?.subject == "test-subject" && "blog-admin" in session.groups) {
                "Admin session did not persist through PostgreSQL"
            }
            database.revokeAdminSession(sessionHash)
            check(database.findAdminSession(sessionHash) == null) { "Revoked admin session was still accepted" }
            readerAccountFlow(database)
            println("postgres_admin_and_reader_auth=passed")
        } finally {
            database.close()
        }
    }

    private fun readerAccountFlow(database: Database) {
        FakeSmtpServer().use { smtp ->
            val readers = ReaderService(
                database,
                mapOf(
                    "SMTP_HOST" to "127.0.0.1",
                    "SMTP_PORT" to smtp.port.toString(),
                    "SMTP_FROM" to "test@example.test",
                    "WEB_BASE_URL" to "https://blog.example.test/ljblog",
                ),
            )
            val email = "reader-${UUID.randomUUID()}@example.test"
            val password = "reader-password-test-2026"
            check(readers.register(email, password)) { "Reader account registration failed" }
            val verificationToken = tokenFrom(smtp.awaitMessage(), "verify")
            check(readers.verifyEmail(verificationToken)) { "Email verification failed" }
            check(!readers.verifyEmail(verificationToken)) { "Email verification token was reusable" }
            check(readers.login(email, "incorrect-password") == null) { "Incorrect reader password was accepted" }

            val originalSession = readers.login(email, password)
                ?: error("Verified reader could not sign in with the correct password")
            check(readers.account(originalSession.token)?.email == email.lowercase()) { "Reader session was not recognized" }

            readers.requestPasswordReset(email)
            val resetToken = tokenFrom(smtp.awaitMessage(), "reset")
            check(readers.resetPassword(resetToken, "reader-password-updated-2026")) { "Reader password reset failed" }
            check(!readers.resetPassword(resetToken, "reader-password-replayed-2026")) { "Password reset token was reusable" }
            check(readers.account(originalSession.token) == null) { "Password reset did not revoke existing reader sessions" }
            check(readers.login(email, password) == null) { "Old password still worked after reset" }

            val newSession = readers.login(email, "reader-password-updated-2026")
                ?: error("Reader could not sign in after password reset")
            readers.logout(newSession.token)
            check(readers.account(newSession.token) == null) { "Reader logout did not revoke the session" }
        }
    }

    private fun tokenFrom(message: String, parameter: String): String {
        val match = Regex("${Regex.escape(parameter)}=([A-Za-z0-9_-]+)").find(message)
            ?: error("Test email did not contain a $parameter token")
        return match.groupValues[1]
    }

    private class FakeSmtpServer : AutoCloseable {
        private val server = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
        private val messages = LinkedBlockingQueue<String>()
        val port: Int get() = server.localPort
        private val worker = thread(isDaemon = true, name = "auth-smoke-smtp") {
            while (!server.isClosed) runCatching { server.accept().use(::handle) }
        }

        fun awaitMessage(): String = messages.poll(10, TimeUnit.SECONDS)
            ?: error("No verification or password-reset email arrived at the test SMTP server")

        override fun close() {
            server.close()
            worker.join(1_000)
        }

        private fun handle(socket: Socket) {
            socket.soTimeout = 10_000
            val reader = socket.getInputStream().bufferedReader(StandardCharsets.UTF_8)
            val writer = socket.getOutputStream().bufferedWriter(StandardCharsets.UTF_8)
            fun reply(line: String) { writer.write(line); writer.write("\r\n"); writer.flush() }
            reply("220 localhost ESMTP ready")
            while (true) {
                val command = reader.readLine() ?: return
                when {
                    command.startsWith("EHLO", ignoreCase = true) || command.startsWith("HELO", ignoreCase = true) -> {
                        reply("250-localhost")
                        reply("250 OK")
                    }
                    command.startsWith("MAIL FROM", ignoreCase = true) -> reply("250 sender accepted")
                    command.startsWith("RCPT TO", ignoreCase = true) -> reply("250 recipient accepted")
                    command.equals("DATA", ignoreCase = true) -> {
                        reply("354 end with dot")
                        val body = StringBuilder()
                        while (true) {
                            val line = reader.readLine() ?: return
                            if (line == ".") break
                            body.append(line).append('\n')
                        }
                        messages.offer(body.toString())
                        reply("250 message accepted")
                    }
                    command.equals("QUIT", ignoreCase = true) -> { reply("221 closing connection"); return }
                    else -> reply("250 OK")
                }
            }
        }
    }

    private fun digest(value: String): ByteArray = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
}
