package br.com.jaquesprojetos.blog.backend

import br.com.jaquesprojetos.blog.contracts.AdminPostRequest
import br.com.jaquesprojetos.blog.contracts.CommentModerationItem
import br.com.jaquesprojetos.blog.contracts.PostDetails
import br.com.jaquesprojetos.blog.contracts.PostSummary
import java.sql.Connection
import java.util.UUID

class AdminBlogService(private val database: Database) {
    fun listPosts(published: Boolean?): List<PostSummary> = database.query { connection ->
        val sql = "SELECT id, author, title, subtitle, extract(epoch from created_at) * 1000 AS date, thumbnail, category, is_popular, is_main, is_sponsored FROM posts ${if (published == null) "" else "WHERE is_published = ?"} ORDER BY created_at DESC LIMIT 100"
        connection.prepareStatement(sql).use { statement ->
            if (published != null) statement.setBoolean(1, published)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(
                        PostSummary(
                            id = rows.getString("id"), author = rows.getString("author"),
                            title = rows.getString("title"), subtitle = rows.getString("subtitle"),
                            date = rows.getDouble("date").toLong(), thumbnail = legacyGhostUrl(rows.getString("thumbnail")),
                            category = rows.getString("category"), popular = rows.getBoolean("is_popular"),
                            main = rows.getBoolean("is_main"), sponsored = rows.getBoolean("is_sponsored"),
                        )
                    )
                }
            }
        }
    }

    fun getPost(rawId: String): PostDetails? = withUuid(rawId) { id -> database.query { connection ->
        connection.prepareStatement("SELECT id, author, title, subtitle, extract(epoch from created_at) * 1000 AS date, thumbnail, category, content, is_popular, is_main, is_sponsored FROM posts WHERE id = ?").use {
            it.setObject(1, id)
            it.executeQuery().use { rows -> if (rows.next()) rows.toPostDetails() else null }
        }
    } }

    fun createDraft(author: String, post: AdminPostRequest): String? {
        if (!valid(post)) return null
        return database.query { connection ->
            connection.prepareStatement("INSERT INTO posts(author, title, subtitle, thumbnail, content, category, is_popular, is_main, is_sponsored, is_published) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, false) RETURNING id").use {
                it.setString(1, author.take(254))
                it.setString(2, post.title.trim())
                it.setString(3, post.subtitle.trim())
                it.setString(4, post.thumbnail.trim())
                it.setString(5, post.content)
                it.setString(6, post.category)
                it.setBoolean(7, post.popular)
                it.setBoolean(8, post.main)
                it.setBoolean(9, post.sponsored)
                it.executeQuery().use { rows -> if (rows.next()) rows.getString(1) else null }
            }
        }
    }

    fun updatePost(rawId: String, post: AdminPostRequest): Boolean {
        if (!valid(post)) return false
        return withUuid(rawId) { id -> database.query { connection ->
            connection.prepareStatement("UPDATE posts SET title = ?, subtitle = ?, thumbnail = ?, content = ?, category = ?, is_popular = ?, is_main = ?, is_sponsored = ? WHERE id = ?").use {
                it.setString(1, post.title.trim())
                it.setString(2, post.subtitle.trim())
                it.setString(3, post.thumbnail.trim())
                it.setString(4, post.content)
                it.setString(5, post.category)
                it.setBoolean(6, post.popular)
                it.setBoolean(7, post.main)
                it.setBoolean(8, post.sponsored)
                it.setObject(9, id)
                it.executeUpdate() == 1
            }
        } } ?: false
    }

    fun publish(rawId: String): Boolean = withUuid(rawId) { id -> database.query { connection ->
        connection.prepareStatement("UPDATE posts SET is_published = true WHERE id = ? AND is_published = false").use {
            it.setObject(1, id)
            it.executeUpdate() == 1
        }
    } } ?: false

    fun deletePosts(rawIds: List<String>): Int {
        val ids = rawIds.take(100).mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }.distinct()
        if (ids.isEmpty()) return 0
        return database.query { connection ->
            connection.prepareStatement("DELETE FROM posts WHERE id = ANY (?)").use {
                it.setArray(1, connection.createArrayOf("uuid", ids.toTypedArray()))
                it.executeUpdate()
            }
        }
    }

    fun pendingComments(): List<CommentModerationItem> = database.query { connection ->
        connection.prepareStatement("SELECT c.id, c.post_id, a.email, c.body, extract(epoch from c.created_at) * 1000 AS created_at FROM comments c JOIN reader_accounts a ON a.id = c.account_id WHERE c.status = 'pending' ORDER BY c.created_at ASC LIMIT 200").use {
            it.executeQuery().use { rows -> buildList {
                while (rows.next()) add(CommentModerationItem(rows.getString("id"), rows.getString("post_id"), rows.getString("email"), rows.getString("body"), rows.getDouble("created_at").toLong()))
            } }
        }
    }

    fun moderateComment(rawId: String, decision: String): Boolean {
        if (decision !in setOf("approved", "rejected")) return false
        return withUuid(rawId) { id -> database.query { connection ->
            connection.prepareStatement("UPDATE comments SET status = ? WHERE id = ? AND status = 'pending'").use {
                it.setString(1, decision)
                it.setObject(2, id)
                it.executeUpdate() == 1
            }
        } } ?: false
    }

    private fun valid(post: AdminPostRequest): Boolean =
        post.title.trim().length in 1..300 && post.subtitle.length <= 1_000 &&
            post.thumbnail.length <= 2_000 && post.content.length in 1..500_000 &&
            post.category in setOf("Technology", "Programming", "Design")

    private fun <T> withUuid(raw: String, block: (UUID) -> T): T? =
        runCatching { UUID.fromString(raw) }.getOrNull()?.let(block)
}

private fun java.sql.ResultSet.toPostDetails(): PostDetails = PostDetails(
    id = getString("id"), author = getString("author"), title = getString("title"),
    subtitle = getString("subtitle"), date = getDouble("date").toLong(),
    thumbnail = legacyGhostUrl(getString("thumbnail")), content = legacyGhostUrl(getString("content")),
    category = getString("category"), popular = getBoolean("is_popular"),
    main = getBoolean("is_main"), sponsored = getBoolean("is_sponsored"),
)
