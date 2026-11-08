package br.com.jaquesprojetos.blog.backend

import br.com.jaquesprojetos.blog.contracts.DraftReviewResponse
import br.com.jaquesprojetos.blog.contracts.ReviewTaskDetails
import br.com.jaquesprojetos.blog.contracts.SavedDraftResponse
import br.com.jaquesprojetos.blog.contracts.TarsTaskClaim
import br.com.jaquesprojetos.blog.contracts.TarsTaskResult
import java.sql.Connection
import java.util.UUID
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TarsService(private val database: Database) {
    fun enqueue(postId: String, actorId: String, idempotencyKey: String): DraftReviewResponse? {
        val postUuid = uuid(postId) ?: return null
        if (idempotencyKey.length !in 8..128 || actorId.length > 254) return null
        val idempotencyActor = "tars-review:$actorId"
        return database.query { connection -> transaction(connection) {
            val original = connection.prepareStatement("SELECT content FROM posts WHERE id = ? AND is_published = false").use {
                it.setObject(1, postUuid)
                it.executeQuery().use { rows -> if (rows.next()) rows.getString(1) else null }
            } ?: return@transaction null

            val inserted = connection.prepareStatement("INSERT INTO idempotency_records(actor_id, idempotency_key) VALUES (?, ?) ON CONFLICT DO NOTHING").use {
                it.setString(1, idempotencyActor)
                it.setString(2, idempotencyKey)
                it.executeUpdate() == 1
            }
            if (!inserted) {
                return@transaction connection.prepareStatement("SELECT response::text FROM idempotency_records WHERE actor_id = ? AND idempotency_key = ?").use {
                    it.setString(1, idempotencyActor)
                    it.setString(2, idempotencyKey)
                    it.executeQuery().use { rows ->
                        if (rows.next()) rows.getString(1)?.let { stored -> Json.decodeFromString<DraftReviewResponse>(stored) } else null
                    }
                }
            }

            val taskId = connection.prepareStatement("INSERT INTO tars_tasks(post_id, requested_by, original_content) VALUES (?, ?, ?) RETURNING id").use {
                it.setObject(1, postUuid)
                it.setString(2, actorId)
                it.setString(3, original)
                it.executeQuery().use { rows -> rows.next(); rows.getString(1) }
            }
            val response = DraftReviewResponse(taskId, "queued")
            val payload = Json.encodeToString(mapOf("taskId" to taskId, "postId" to postId))
            connection.prepareStatement("INSERT INTO outbox_events(event_type, aggregate_id, payload) VALUES ('tars.review.requested', ?, ?::jsonb)").use {
                it.setObject(1, UUID.fromString(taskId))
                it.setString(2, payload)
                it.executeUpdate()
            }
            connection.prepareStatement("UPDATE idempotency_records SET response = ?::jsonb WHERE actor_id = ? AND idempotency_key = ?").use {
                it.setString(1, Json.encodeToString(response))
                it.setString(2, idempotencyActor)
                it.setString(3, idempotencyKey)
                it.executeUpdate()
            }
            response
        } }
    }

    fun claim(limit: Int = 3): List<TarsTaskClaim> = database.query { connection -> transaction(connection) {
        val claimed = mutableListOf<TarsTaskClaim>()
        connection.prepareStatement(
            "SELECT e.id AS event_id, t.id AS task_id, t.post_id, t.original_content FROM outbox_events e JOIN tars_tasks t ON t.id::text = e.payload->>'taskId' WHERE e.event_type = 'tars.review.requested' AND e.delivered_at IS NULL AND e.available_at <= now() AND (e.claimed_until IS NULL OR e.claimed_until <= now()) AND t.status IN ('queued', 'processing') AND (t.lease_until IS NULL OR t.lease_until <= now()) ORDER BY e.id LIMIT ? FOR UPDATE OF e, t SKIP LOCKED"
        ).use { select ->
            select.setInt(1, limit.coerceIn(1, 5))
            select.executeQuery().use { rows ->
                while (rows.next()) {
                    val eventId = rows.getLong("event_id")
                    val taskId = rows.getObject("task_id", UUID::class.java)
                    val postId = rows.getObject("post_id", UUID::class.java)
                    val content = rows.getString("original_content")
                    connection.prepareStatement("UPDATE outbox_events SET claimed_until = now() + interval '2 minutes' WHERE id = ?").use {
                        it.setLong(1, eventId)
                        it.executeUpdate()
                    }
                    connection.prepareStatement("UPDATE tars_tasks SET status = 'processing', lease_until = now() + interval '2 minutes', attempts = attempts + 1, updated_at = now() WHERE id = ?").use {
                        it.setObject(1, taskId)
                        it.executeUpdate()
                    }
                    claimed += TarsTaskClaim(taskId.toString(), postId.toString(), content)
                }
            }
        }
        claimed
    } }

    fun complete(result: TarsTaskResult): Boolean {
        val taskId = uuid(result.taskId) ?: return false
        val revisedContent = result.revisedContent
        if (result.error.isNullOrBlank() && (revisedContent.isNullOrBlank() || revisedContent.length > 500_000)) return false
        return database.query { connection -> transaction(connection) {
            val status = if (result.error.isNullOrBlank()) "review" else "failed"
            val updated = connection.prepareStatement("UPDATE tars_tasks SET status = ?, revised_content = ?, error_message = ?, lease_until = NULL, updated_at = now() WHERE id = ? AND status = 'processing' AND lease_until > now() RETURNING id").use {
                it.setString(1, status)
                it.setString(2, result.revisedContent)
                it.setString(3, result.error?.take(1000))
                it.setObject(4, taskId)
                it.executeQuery().use { rows -> rows.next() }
            }
            if (!updated) return@transaction false
            if (result.error.isNullOrBlank()) {
                connection.prepareStatement("UPDATE outbox_events SET delivered_at = now(), claimed_until = NULL WHERE aggregate_id = ? AND event_type = 'tars.review.requested'").use {
                    it.setObject(1, taskId)
                    it.executeUpdate()
                }
            } else {
                connection.prepareStatement("UPDATE tars_tasks SET status = CASE WHEN attempts >= 3 THEN 'failed' ELSE 'queued' END WHERE id = ?").use {
                    it.setObject(1, taskId)
                    it.executeUpdate()
                }
                connection.prepareStatement("UPDATE outbox_events SET claimed_until = NULL, available_at = now() + interval '30 seconds' WHERE aggregate_id = ? AND event_type = 'tars.review.requested'").use {
                    it.setObject(1, taskId)
                    it.executeUpdate()
                }
            }
            true
        } }
    }

    fun details(taskId: String): ReviewTaskDetails? = uuid(taskId)?.let { id -> database.query { connection ->
        connection.prepareStatement("SELECT id, post_id, status, original_content, revised_content FROM tars_tasks WHERE id = ?").use {
            it.setObject(1, id)
            it.executeQuery().use { rows -> if (rows.next()) ReviewTaskDetails(rows.getString("id"), rows.getString("post_id"), rows.getString("status"), rows.getString("original_content"), rows.getString("revised_content")) else null }
        }
    } }

    fun saveDraft(taskId: String, author: String, idempotencyKey: String): SavedDraftResponse? = uuid(taskId)?.let { id -> database.query { connection -> transaction(connection) {
        if (idempotencyKey.length !in 8..128 || author.length > 254) return@transaction null
        val idempotencyActor = "tars-save:$author"
        val prior = savedDraftIdempotency(connection, idempotencyActor, idempotencyKey)
        if (prior.first) return@transaction prior.second
        val row = connection.prepareStatement("SELECT t.post_id, t.revised_content, p.author, p.title, p.subtitle, p.thumbnail, p.category, p.is_popular, p.is_main, p.is_sponsored FROM tars_tasks t JOIN posts p ON p.id = t.post_id WHERE t.id = ? AND t.status = 'review' FOR UPDATE OF t").use {
            it.setObject(1, id)
            it.executeQuery().use { rows -> if (rows.next()) DraftSource(rows.getObject("post_id", UUID::class.java), rows.getString("revised_content"), rows.getString("title"), rows.getString("subtitle"), rows.getString("thumbnail"), rows.getString("category"), rows.getBoolean("is_popular"), rows.getBoolean("is_main"), rows.getBoolean("is_sponsored")) else null }
        } ?: return@transaction savedDraftIdempotency(connection, idempotencyActor, idempotencyKey).second
        val insertedKey = connection.prepareStatement("INSERT INTO idempotency_records(actor_id, idempotency_key) VALUES (?, ?) ON CONFLICT DO NOTHING").use {
            it.setString(1, idempotencyActor); it.setString(2, idempotencyKey); it.executeUpdate() == 1
        }
        if (!insertedKey) return@transaction savedDraftIdempotency(connection, idempotencyActor, idempotencyKey).second
        val draftId = connection.prepareStatement("INSERT INTO posts(author, title, subtitle, thumbnail, content, category, is_popular, is_main, is_sponsored, is_published) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, false) RETURNING id").use {
            it.setString(1, author.take(254)); it.setString(2, row.title); it.setString(3, row.subtitle); it.setString(4, row.thumbnail)
            it.setString(5, row.revisedContent); it.setString(6, row.category); it.setBoolean(7, row.popular); it.setBoolean(8, row.main); it.setBoolean(9, row.sponsored)
            it.executeQuery().use { rows -> rows.next(); rows.getString(1) }
        }
        connection.prepareStatement("UPDATE tars_tasks SET status = 'completed', updated_at = now() WHERE id = ?").use {
            it.setObject(1, id); it.executeUpdate()
        }
        val response = SavedDraftResponse(draftId, "draft")
        connection.prepareStatement("UPDATE idempotency_records SET response = ?::jsonb WHERE actor_id = ? AND idempotency_key = ?").use {
            it.setString(1, Json.encodeToString(response)); it.setString(2, idempotencyActor); it.setString(3, idempotencyKey); it.executeUpdate()
        }
        response
    } } }

    private fun savedDraftIdempotency(connection: Connection, actor: String, key: String): Pair<Boolean, SavedDraftResponse?> =
        connection.prepareStatement("SELECT response::text FROM idempotency_records WHERE actor_id = ? AND idempotency_key = ?").use {
            it.setString(1, actor); it.setString(2, key)
            it.executeQuery().use { rows ->
                if (!rows.next()) false to null
                else true to rows.getString(1)?.let { json -> Json.decodeFromString<SavedDraftResponse>(json) }
            }
        }

    private fun <T> transaction(connection: Connection, block: () -> T): T {
        connection.autoCommit = false
        try {
            val result = block()
            connection.commit()
            return result
        } catch (failure: Exception) {
            connection.rollback()
            throw failure
        } finally {
            connection.autoCommit = true
        }
    }

    private fun uuid(value: String): UUID? = runCatching { UUID.fromString(value) }.getOrNull()
    private data class DraftSource(val postId: UUID, val revisedContent: String, val title: String, val subtitle: String, val thumbnail: String, val category: String, val popular: Boolean, val main: Boolean, val sponsored: Boolean)
}
