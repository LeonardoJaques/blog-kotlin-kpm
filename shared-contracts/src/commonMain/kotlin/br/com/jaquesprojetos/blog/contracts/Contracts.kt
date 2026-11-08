package br.com.jaquesprojetos.blog.contracts

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ErrorResponse(val code: String, val message: String)

@Serializable
data class HealthResponse(val status: String)

@Serializable
data class PostSummary(
    @SerialName("_id") val id: String,
    val author: String,
    val title: String,
    val subtitle: String,
    val date: Long,
    val thumbnail: String,
    val category: String,
    val popular: Boolean,
    val main: Boolean,
    val sponsored: Boolean,
)

@Serializable
data class PostDetails(
    @SerialName("_id") val id: String,
    val author: String,
    val title: String,
    val subtitle: String,
    val date: Long,
    val thumbnail: String,
    val content: String,
    val category: String,
    val popular: Boolean,
    val main: Boolean,
    val sponsored: Boolean,
)

@Serializable
data class PostDetailsResponse(val data: PostDetails)

@Serializable
data class PostListResponse(val data: List<PostSummary>)

@Serializable
data class ReaderRegistrationRequest(val email: String, val password: String)

@Serializable
data class ReaderLoginRequest(val email: String, val password: String)

@Serializable
data class VerifyEmailRequest(val token: String)

@Serializable
data class PasswordResetRequest(val email: String)

@Serializable
data class EmailRequest(val email: String)

@Serializable
data class PasswordResetCompleteRequest(val token: String, val password: String)

@Serializable
data class ReaderSessionResponse(val id: String, val email: String)

@Serializable
data class NewsletterSubscriptionRequest(val email: String)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class FavoriteRequest(val postId: String)

@Serializable
data class CommentRequest(val postId: String, val body: String)

@Serializable
data class CommentResponse(val id: String, val postId: String, val body: String, val status: String)

@Serializable
data class DraftReviewRequest(
    val postId: String,
    val idempotencyKey: String,
)

@Serializable
data class DraftReviewResponse(val taskId: String, val status: String)

@Serializable
data class AdminSessionResponse(val subject: String)

@Serializable
data class AdminPostRequest(
    val title: String,
    val subtitle: String,
    val thumbnail: String,
    val content: String,
    val category: String,
    val popular: Boolean = false,
    val main: Boolean = false,
    val sponsored: Boolean = false,
)

@Serializable
data class CommentModerationRequest(val decision: String)

@Serializable
data class CommentModerationItem(val id: String, val postId: String, val email: String, val body: String, val createdAt: Long)

@Serializable
data class TarsTaskClaim(val taskId: String, val postId: String, val content: String)

@Serializable
data class TarsTaskResult(val taskId: String, val revisedContent: String? = null, val error: String? = null)

@Serializable
data class TarsReviewRequest(val taskId: String, val content: String)

@Serializable
data class TarsReviewResponse(val revisedContent: String)

@Serializable
data class ReviewTaskDetails(val taskId: String, val postId: String, val status: String, val originalContent: String, val revisedContent: String?)

@Serializable
data class SavedDraftResponse(val postId: String, val status: String)
