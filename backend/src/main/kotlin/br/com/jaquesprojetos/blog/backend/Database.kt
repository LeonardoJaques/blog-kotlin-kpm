package br.com.jaquesprojetos.blog.backend

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class Database(environment: Map<String, String> = System.getenv()) : AutoCloseable, AdminAuthStore {
    private val source = HikariDataSource(HikariConfig().apply {
        jdbcUrl = environment["DATABASE_URL"] ?: "jdbc:postgresql://localhost:5432/blog"
        username = environment["DATABASE_USER"] ?: "blog"
        password = environment["DATABASE_PASSWORD"] ?: error("DATABASE_PASSWORD must be configured")
        maximumPoolSize = environment["DATABASE_POOL_SIZE"]?.toIntOrNull()?.coerceIn(2, 64) ?: 12
        minimumIdle = 1
        connectionTimeout = 3_000
        validationTimeout = 1_000
        poolName = "blog-postgres"
    })

    init {
        Flyway.configure()
            .dataSource(source)
            .locations("classpath:db/migration")
            .validateMigrationNaming(true)
            .load()
            .migrate()
    }

    fun <T> query(block: (Connection) -> T): T = source.connection.use(block)

    override fun insertOidcTransaction(stateHash: ByteArray, nonceHash: ByteArray, verifier: String) {
        query { connection ->
            connection.prepareStatement(
                "INSERT INTO oidc_transactions(state_hash, nonce_hash, pkce_verifier, expires_at) VALUES (?, ?, ?, now() + interval '10 minutes')"
            ).use {
                it.setBytes(1, stateHash)
                it.setBytes(2, nonceHash)
                it.setString(3, verifier)
                it.executeUpdate()
            }
            connection.prepareStatement("DELETE FROM oidc_transactions WHERE expires_at <= now()").use { it.executeUpdate() }
        }
    }

    override fun consumeOidcTransaction(stateHash: ByteArray): OidcTransactionRecord? = query { connection ->
        connection.autoCommit = false
        try {
            val row = connection.prepareStatement(
                "DELETE FROM oidc_transactions WHERE state_hash = ? AND expires_at > now() RETURNING nonce_hash, pkce_verifier"
            ).use {
                it.setBytes(1, stateHash)
                it.executeQuery().use { rows ->
                    if (rows.next()) OidcTransactionRecord(rows.getBytes(1), rows.getString(2)) else null
                }
            }
            connection.commit()
            row
        } catch (failure: Exception) {
            connection.rollback()
            throw failure
        } finally {
            connection.autoCommit = true
        }
    }

    override fun insertAdminSession(tokenHash: ByteArray, subject: String, groups: Set<String>, expiresAt: Instant) {
        query { connection ->
            connection.prepareStatement("INSERT INTO admin_sessions(id_hash, subject, groups, expires_at) VALUES (?, ?, ?, ?)").use {
                it.setBytes(1, tokenHash)
                it.setString(2, subject)
                it.setArray(3, connection.createArrayOf("text", groups.toTypedArray()))
                it.setObject(4, postgresTimestamptz(expiresAt))
                it.executeUpdate()
            }
        }
    }

    override fun findAdminSession(tokenHash: ByteArray): StoredAdminSession? = query { connection ->
        connection.prepareStatement("SELECT subject, groups FROM admin_sessions WHERE id_hash = ? AND revoked_at IS NULL AND expires_at > now()").use {
            it.setBytes(1, tokenHash)
            it.executeQuery().use { rows ->
                if (!rows.next()) null
                else {
                    val groups = (rows.getArray("groups").array as Array<*>).map { value -> value.toString() }.toSet()
                    StoredAdminSession(rows.getString("subject"), groups)
                }
            }
        }
    }

    override fun revokeAdminSession(tokenHash: ByteArray) {
        query { connection ->
            connection.prepareStatement("UPDATE admin_sessions SET revoked_at = now() WHERE id_hash = ? AND revoked_at IS NULL").use {
                it.setBytes(1, tokenHash)
                it.executeUpdate()
            }
        }
    }

    fun ready(): Boolean = runCatching {
        query { connection -> connection.createStatement().use { it.execute("SELECT 1") } }
        true
    }.getOrDefault(false)

    override fun close() = source.close()
}

internal fun postgresTimestamptz(value: Instant): OffsetDateTime = value.atOffset(ZoneOffset.UTC)
