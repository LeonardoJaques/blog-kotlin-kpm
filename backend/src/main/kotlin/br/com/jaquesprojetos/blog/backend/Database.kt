package br.com.jaquesprojetos.blog.backend

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Connection

class Database(environment: Map<String, String> = System.getenv()) : AutoCloseable {
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

    fun ready(): Boolean = runCatching {
        query { connection -> connection.createStatement().use { it.execute("SELECT 1") } }
        true
    }.getOrDefault(false)

    override fun close() = source.close()
}
