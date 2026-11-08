package br.com.jaquesprojetos.blog.backend

import br.com.jaquesprojetos.blog.contracts.CommentResponse
import com.sun.mail.smtp.SMTPTransport
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.Connection
import java.util.Base64
import java.util.Properties
import java.util.UUID

class ReaderService(
    private val database: Database,
    private val environment: Map<String, String> = System.getenv(),
) {
    fun register(email: String, password: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        val passwordHash = Passwords.hash(password)
        val token = randomToken()
        val accountId = database.query { connection ->
            connection.prepareStatement(
                "INSERT INTO reader_accounts(email, password_hash) VALUES (?, ?) ON CONFLICT (email) DO NOTHING RETURNING id"
            ).use { statement ->
                statement.setString(1, normalizedEmail)
                statement.setString(2, passwordHash)
                statement.executeQuery().use { rows -> if (rows.next()) rows.getObject(1, UUID::class.java) else null }
            }
        } ?: return false

        database.query { connection -> insertToken(connection, accountId, "verify_email", token, "2 hours") }
        sendMail(
            normalizedEmail,
            "Verify your blog account",
            "Open ${webUrl()}/account?verify=$token and confirm your email address.",
        )
        return true
    }

    fun verifyEmail(token: String): Boolean = consumeToken(token, "verify_email") { connection, accountId ->
        connection.prepareStatement("UPDATE reader_accounts SET email_verified_at = now() WHERE id = ?").use {
            it.setObject(1, accountId)
            it.executeUpdate() == 1
        }
    }

    fun login(email: String, password: String): ReaderSession? {
        val found = database.query { connection ->
            connection.prepareStatement("SELECT id, email, password_hash, email_verified_at IS NOT NULL AS verified FROM reader_accounts WHERE email = ?").use { statement ->
                statement.setString(1, email.trim().lowercase())
                statement.executeQuery().use { rows ->
                    if (!rows.next()) null else AccountPassword(
                        rows.getObject("id", UUID::class.java), rows.getString("email"),
                        rows.getString("password_hash"), rows.getBoolean("verified"),
                    )
                }
            }
        } ?: run {
            Passwords.verify(password, DUMMY_PASSWORD_HASH)
            return null
        }
        val passwordMatches = Passwords.verify(password, found.passwordHash)
        if (!passwordMatches || !found.verified) return null

        val sessionToken = randomToken()
        database.query { connection ->
            connection.prepareStatement("INSERT INTO reader_sessions(id_hash, account_id, expires_at) VALUES (?, ?, now() + interval '30 days')").use {
                it.setBytes(1, tokenHash(sessionToken))
                it.setObject(2, found.id)
                it.executeUpdate()
            }
        }
        return ReaderSession(sessionToken, found.id, found.email)
    }

    fun account(sessionToken: String?): ReaderSession? {
        if (sessionToken.isNullOrBlank()) return null
        return database.query { connection ->
            connection.prepareStatement(
                "SELECT a.id, a.email FROM reader_sessions s JOIN reader_accounts a ON a.id = s.account_id WHERE s.id_hash = ? AND s.revoked_at IS NULL AND s.expires_at > now() AND a.email_verified_at IS NOT NULL"
            ).use { statement ->
                statement.setBytes(1, tokenHash(sessionToken))
                statement.executeQuery().use { rows ->
                    if (rows.next()) ReaderSession(sessionToken, rows.getObject("id", UUID::class.java), rows.getString("email")) else null
                }
            }
        }
    }

    fun logout(sessionToken: String?) {
        if (sessionToken.isNullOrBlank()) return
        database.query { connection ->
            connection.prepareStatement("UPDATE reader_sessions SET revoked_at = now() WHERE id_hash = ? AND revoked_at IS NULL").use {
                it.setBytes(1, tokenHash(sessionToken))
                it.executeUpdate()
            }
        }
    }

    fun requestPasswordReset(email: String) {
        val accountId = database.query { connection ->
            connection.prepareStatement("SELECT id FROM reader_accounts WHERE email = ?").use { statement ->
                statement.setString(1, email.trim().lowercase())
                statement.executeQuery().use { rows -> if (rows.next()) rows.getObject(1, UUID::class.java) else null }
            }
        } ?: return
        val token = randomToken()
        database.query { connection -> insertToken(connection, accountId, "reset_password", token, "1 hour") }
        sendMail(email.trim().lowercase(), "Reset your blog password", "Open ${webUrl()}/account?reset=$token to choose a new password.")
    }

    fun requestVerification(email: String) {
        val normalized = email.trim().lowercase()
        val account = database.query { connection ->
            connection.prepareStatement("SELECT id FROM reader_accounts WHERE email = ? AND email_verified_at IS NULL").use {
                it.setString(1, normalized)
                it.executeQuery().use { rows -> if (rows.next()) rows.getObject(1, UUID::class.java) else null }
            }
        } ?: return
        val token = randomToken()
        database.query { connection -> insertToken(connection, account, "verify_email", token, "2 hours") }
        sendMail(normalized, "Verify your blog account", "Open ${webUrl()}/account?verify=$token and confirm your email address.")
    }

    fun resetPassword(token: String, password: String): Boolean {
        val hash = Passwords.hash(password)
        return consumeToken(token, "reset_password") { connection, accountId ->
            val updated = connection.prepareStatement("UPDATE reader_accounts SET password_hash = ? WHERE id = ?").use {
                it.setString(1, hash)
                it.setObject(2, accountId)
                it.executeUpdate() == 1
            }
            if (updated) connection.prepareStatement("UPDATE reader_sessions SET revoked_at = now() WHERE account_id = ? AND revoked_at IS NULL").use {
                it.setObject(1, accountId)
                it.executeUpdate()
            }
            updated
        }
    }

    fun addFavorite(accountId: UUID, postId: String): Boolean = withPostId(postId) { id -> database.query { connection ->
        connection.prepareStatement("INSERT INTO favorites(account_id, post_id) SELECT ?, id FROM posts WHERE id = ? AND is_published ON CONFLICT DO NOTHING").use {
            it.setObject(1, accountId)
            it.setObject(2, id)
            it.executeUpdate() > 0
        }
    } } ?: false

    fun removeFavorite(accountId: UUID, postId: String): Boolean = withPostId(postId) { id -> database.query { connection ->
        connection.prepareStatement("DELETE FROM favorites WHERE account_id = ? AND post_id = ?").use {
            it.setObject(1, accountId)
            it.setObject(2, id)
            it.executeUpdate() > 0
        }
    } } ?: false

    fun favoriteIds(accountId: UUID): List<String> = database.query { connection ->
        connection.prepareStatement("SELECT post_id FROM favorites WHERE account_id = ? ORDER BY created_at DESC").use {
            it.setObject(1, accountId)
            it.executeQuery().use { rows -> buildList { while (rows.next()) add(rows.getString(1)) } }
        }
    }

    fun addComment(accountId: UUID, postId: String, body: String): CommentResponse? {
        if (body.isBlank() || body.length > 5_000) return null
        return withPostId(postId) { id -> database.query { connection ->
            connection.prepareStatement("INSERT INTO comments(account_id, post_id, body) SELECT ?, id, ? FROM posts WHERE id = ? AND is_published RETURNING id, status").use {
                it.setObject(1, accountId)
                it.setString(2, body.trim())
                it.setObject(3, id)
                it.executeQuery().use { rows -> if (rows.next()) CommentResponse(rows.getString("id"), postId, body.trim(), rows.getString("status")) else null }
            }
        } }
    }

    fun approvedComments(postId: String): List<CommentResponse> = withPostId(postId) { id -> database.query { connection ->
        connection.prepareStatement("SELECT c.id, c.body, c.status FROM comments c JOIN posts p ON p.id = c.post_id WHERE c.post_id = ? AND p.is_published AND c.status = 'approved' ORDER BY c.created_at").use {
            it.setObject(1, id)
            it.executeQuery().use { rows -> buildList { while (rows.next()) add(CommentResponse(rows.getString("id"), postId, rows.getString("body"), rows.getString("status"))) } }
        }
    } } ?: emptyList()

    private fun consumeToken(token: String, purpose: String, action: (Connection, UUID) -> Boolean): Boolean = database.query { connection ->
        connection.autoCommit = false
        try {
            val accountId = connection.prepareStatement("UPDATE reader_tokens SET consumed_at = now() WHERE token_hash = ? AND purpose = ? AND consumed_at IS NULL AND expires_at > now() RETURNING account_id").use {
                it.setBytes(1, tokenHash(token))
                it.setString(2, purpose)
                it.executeQuery().use { rows -> if (rows.next()) rows.getObject(1, UUID::class.java) else null }
            }
            if (accountId == null) {
                connection.rollback()
                false
            } else {
                val result = action(connection, accountId)
                if (result) connection.commit() else connection.rollback()
                result
            }
        } catch (failure: Exception) {
            connection.rollback()
            throw failure
        } finally {
            connection.autoCommit = true
        }
    }

    private fun insertToken(connection: Connection, accountId: UUID, purpose: String, token: String, validity: String) {
        val interval = if (validity == "2 hours") "2 hours" else "1 hour"
        connection.prepareStatement("INSERT INTO reader_tokens(token_hash, account_id, purpose, expires_at) VALUES (?, ?, ?, now() + interval '$interval')").use {
            it.setBytes(1, tokenHash(token))
            it.setObject(2, accountId)
            it.setString(3, purpose)
            it.executeUpdate()
        }
    }

    private fun sendMail(to: String, subject: String, body: String) {
        val host = environment["SMTP_HOST"] ?: return
        val port = environment["SMTP_PORT"]?.toIntOrNull() ?: 1025
        val properties = Properties().apply {
            setProperty("mail.smtp.host", host)
            setProperty("mail.smtp.port", port.toString())
            setProperty("mail.smtp.auth", (environment["SMTP_USER"] != null).toString())
            setProperty("mail.smtp.starttls.enable", (environment["SMTP_STARTTLS"]?.toBooleanStrictOrNull() ?: false).toString())
            setProperty("mail.smtp.starttls.required", (environment["SMTP_STARTTLS"]?.toBooleanStrictOrNull() ?: false).toString())
            setProperty("mail.smtp.connectiontimeout", "3000")
            setProperty("mail.smtp.timeout", "3000")
            setProperty("mail.smtp.writetimeout", "3000")
        }
        val message = MimeMessage(Session.getInstance(properties)).apply {
            setFrom(InternetAddress(environment["SMTP_FROM"] ?: "no-reply@blog.local"))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            setSubject(subject, "UTF-8")
            setText(body, "UTF-8")
        }
        val transport = SMTPTransport(Session.getInstance(properties), null)
        try {
            transport.connect(host, port, environment["SMTP_USER"], environment["SMTP_PASSWORD"])
            transport.sendMessage(message, message.allRecipients)
        } finally {
            transport.close()
        }
    }

    private fun publicUrl() = environment["PUBLIC_BASE_URL"]?.trimEnd('/') ?: "http://localhost:8081"
    private fun webUrl() = environment["WEB_BASE_URL"]?.trimEnd('/') ?: "http://localhost:8080"

    private fun <T> withPostId(rawId: String, block: (UUID) -> T): T? =
        runCatching { UUID.fromString(rawId) }.getOrNull()?.let(block)

    private fun tokenHash(token: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())

    private fun randomToken(): String = ByteArray(32).also(random::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

    private data class AccountPassword(val id: UUID, val email: String, val passwordHash: String, val verified: Boolean)
    data class ReaderSession(val token: String, val id: UUID, val email: String)

    private companion object {
        val random = SecureRandom()
        val DUMMY_PASSWORD_HASH = Passwords.hash("dummy-password-that-is-never-used")
    }
}

private object Passwords {
    private const val MEMORY_KB = 65_536
    private const val ITERATIONS = 3
    private const val PARALLELISM = 1
    private const val HASH_BYTES = 32
    private val random = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(16).also(random::nextBytes)
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withIterations(ITERATIONS)
            .withMemoryAsKB(MEMORY_KB)
            .withParallelism(PARALLELISM)
            .build()
        val hash = ByteArray(HASH_BYTES)
        Argon2BytesGenerator().apply { init(parameters); generateBytes(password.toCharArray(), hash) }
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return "\$argon2id\$v=19\$m=$MEMORY_KB,t=$ITERATIONS,p=$PARALLELISM\$${encoder.encodeToString(salt)}\$${encoder.encodeToString(hash)}"
    }

    fun verify(password: String, encoded: String): Boolean = runCatching {
        val pieces = encoded.split('$')
        val salt = Base64.getUrlDecoder().decode(pieces[4])
        val expected = Base64.getUrlDecoder().decode(pieces[5])
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt).withIterations(ITERATIONS).withMemoryAsKB(MEMORY_KB).withParallelism(PARALLELISM).build()
        val actual = ByteArray(expected.size)
        Argon2BytesGenerator().apply { init(parameters); generateBytes(password.toCharArray(), actual) }
        MessageDigest.isEqual(expected, actual)
    }.getOrDefault(false)
}
