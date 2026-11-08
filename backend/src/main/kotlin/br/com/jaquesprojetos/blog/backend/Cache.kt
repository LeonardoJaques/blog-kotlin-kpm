package br.com.jaquesprojetos.blog.backend

import io.lettuce.core.RedisClient
import io.lettuce.core.ClientOptions
import io.lettuce.core.SocketOptions
import io.lettuce.core.TimeoutOptions
import java.time.Duration

class Cache(redisUrl: String = System.getenv("REDIS_URL") ?: "redis://localhost:6379/0") : AutoCloseable {
    private val client = RedisClient.create(redisUrl)
    private val redisConnection by lazy { client.connect() }
    private val connection by lazy { redisConnection.sync() }

    init {
        client.setOptions(
            ClientOptions.builder()
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofMillis(500)))
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofMillis(500)).build())
                .build()
        )
    }

    fun read(key: String): String? = runCatching { connection.get(key) }.getOrNull()

    fun write(key: String, value: String) {
        runCatching {
            connection.setex(key, FRESH_SECONDS, value)
            connection.setex("$key:stale", STALE_SECONDS, value)
        }
    }

    fun stale(key: String): String? = read("$key:stale")

    fun postsVersion(): String = read("blog:posts:version") ?: "0"

    fun invalidatePosts() {
        runCatching { connection.set("blog:posts:version", java.util.UUID.randomUUID().toString()) }
    }

    fun increment(key: String, windowSeconds: Long): Long? = runCatching {
        val count = connection.incr(key)
        if (count == 1L) connection.expire(key, windowSeconds)
        count
    }.getOrNull()

    override fun close() {
        runCatching { redisConnection.close() }
        client.shutdown()
    }

    private companion object {
        const val FRESH_SECONDS = 60L
        const val STALE_SECONDS = 600L
    }
}
