package br.com.jaquesprojetos.blog.backend

import br.com.jaquesprojetos.blog.contracts.ErrorResponse
import br.com.jaquesprojetos.blog.contracts.AdminSessionResponse
import br.com.jaquesprojetos.blog.contracts.AdminPostRequest
import br.com.jaquesprojetos.blog.contracts.CommentModerationRequest
import br.com.jaquesprojetos.blog.contracts.HealthResponse
import br.com.jaquesprojetos.blog.contracts.MessageResponse
import br.com.jaquesprojetos.blog.contracts.NewsletterSubscriptionRequest
import br.com.jaquesprojetos.blog.contracts.ReaderLoginRequest
import br.com.jaquesprojetos.blog.contracts.ReaderRegistrationRequest
import br.com.jaquesprojetos.blog.contracts.VerifyEmailRequest
import br.com.jaquesprojetos.blog.contracts.PasswordResetRequest
import br.com.jaquesprojetos.blog.contracts.PasswordResetCompleteRequest
import br.com.jaquesprojetos.blog.contracts.EmailRequest
import br.com.jaquesprojetos.blog.contracts.PostDetails
import br.com.jaquesprojetos.blog.contracts.PostDetailsResponse
import br.com.jaquesprojetos.blog.contracts.PostListResponse
import br.com.jaquesprojetos.blog.contracts.PostSummary
import io.ktor.http.HttpHeaders
import io.ktor.http.Cookie
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

fun Application.module() {
    val appLog = environment.log
    val database = Database()
    val cache = Cache()
    val readers = ReaderService(database)
    val adminAuth = AutheliaOidc(database)
    val adminBlog = AdminBlogService(database)
    val tars = TarsService(database)
    val secureCookies = System.getenv("COOKIE_SECURE")?.toBooleanStrictOrNull() ?: true
    environment.monitor.subscribe(io.ktor.server.application.ApplicationStopped) {
        database.close()
        cache.close()
    }

    install(CallLogging)
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; explicitNulls = false })
    }
    install(CORS) {
        val origins = System.getenv("WEB_ORIGINS")?.split(',')?.map(String::trim)?.filter(String::isNotEmpty).orEmpty()
        origins.forEach { origin ->
            val uri = runCatching { java.net.URI(origin) }.getOrNull()
            val scheme = uri?.scheme
            val authority = uri?.rawAuthority
            if (scheme != null && authority != null && scheme in setOf("http", "https") && uri.rawPath in setOf("", "/") && uri.rawQuery == null && uri.rawFragment == null) {
                allowHost(authority, schemes = listOf(scheme))
            }
        }
        allowCredentials = true
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-CSRF-Token")
        allowHeader("Idempotency-Key")
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }
    install(StatusPages) {
        exception<BadRequestException> { call, _ -> call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", "Invalid request")) }
        exception<Exception> { call, cause ->
            appLog.error("Request failed at ${call.request.path()}", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error", "Request could not be completed"))
        }
    }
    intercept(io.ktor.server.application.ApplicationCallPipeline.Call) {
        call.response.header("X-Content-Type-Options", "nosniff")
        call.response.header("Referrer-Policy", "strict-origin-when-cross-origin")
        call.response.header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
        call.response.header(HttpHeaders.CacheControl, "no-store")
        val path = call.request.path()
        val (limit, windowSeconds, bucket) = when {
            path.startsWith("/api/v1/auth/login") -> Triple(5L, 60L, "auth-login")
            path.startsWith("/api/v1/auth/register") -> Triple(5L, 3_600L, "auth-register")
            path.startsWith("/api/v1/auth/verify-email") -> Triple(5L, 3_600L, "auth-verify")
            path.startsWith("/api/v1/auth/password-reset") -> Triple(5L, 3_600L, path.substringBeforeLast('/'))
            path.startsWith("/api/v1/admin/") -> Triple(60L, 60L, "admin")
            else -> Triple(300L, 60L, path.substringBeforeLast('/'))
        }
        val remoteHost = call.request.local.remoteHost
        val remoteHash = java.security.MessageDigest.getInstance("SHA-256")
            .digest(remoteHost.toByteArray())
            .take(12)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val adminIdentity = if (bucket == "admin") {
            val sessionToken = call.request.cookies["admin_session"]
            onIo { adminAuth.session(sessionToken)?.subject }
        } else null
        val identityHash = adminIdentity?.let { sha256Hex(it) } ?: remoteHash
        val count = onIo { cache.increment("rate:$identityHash:$bucket", windowSeconds) }
        if (count != null && count > limit) {
            call.respond(HttpStatusCode.TooManyRequests, ErrorResponse("rate_limited", "Too many requests"))
            finish()
        }
    }

    routing {
        // Kobweb's production client still probes its development status URL.
        // Return an empty success so the probe does not produce a browser 404.
        get("/api/kobweb-status") { call.respond(HttpStatusCode.NoContent) }

        route("/api/v1") {
            get("/health/live") { call.respond(HealthResponse("live")) }
            get("/health/ready") {
                if (onIo { database.ready() }) call.respond(HealthResponse("ready"))
                else call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse("database_unavailable", "Database is not ready"))
            }
            get("/admin/login") {
                if (!adminAuth.configured()) call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse("oidc_not_configured", "Authelia OIDC is not configured"))
                else call.respondRedirect(onIo { adminAuth.authorizationUrl() })
            }
            adminOidcCallback(adminAuth, secureCookies, System.getenv("WEB_BASE_URL")) { stage, failureType ->
                appLog.warn("Authelia OIDC callback failed at stage: $stage (type: $failureType)")
            }
            get("/admin/session") {
                val session = onIo { adminAuth.session(call.request.cookies["admin_session"]) }
                if (session == null) call.respond(HttpStatusCode.Unauthorized, ErrorResponse("authentication_required", "Sign in with Authelia"))
                else call.respond(AdminSessionResponse(session.subject))
            }
            post("/admin/logout") {
                if (!call.validCsrf("admin_csrf")) call.respond(HttpStatusCode.Forbidden, ErrorResponse("csrf_failed", "Request could not be verified"))
                else {
                    onIo { adminAuth.revoke(call.request.cookies["admin_session"]) }
                    call.response.cookies.append(Cookie("admin_session", "", maxAge = 0, secure = secureCookies, httpOnly = true, extensions = mapOf("SameSite" to "Lax")))
                    call.response.cookies.append(Cookie("admin_csrf", "", maxAge = 0, secure = secureCookies, httpOnly = false, extensions = mapOf("SameSite" to "Lax")))
                    call.respond(HttpStatusCode.NoContent)
                }
            }
            get("/admin/posts") {
                if (call.requireAdmin(adminAuth) == null) return@get
                val published = when (call.request.queryParameters["status"]) {
                    "published" -> true
                    "draft" -> false
                    else -> null
                }
                call.respond(PostListResponse(onIo { adminBlog.listPosts(published) }))
            }
            get("/admin/posts/{id}") {
                if (call.requireAdmin(adminAuth) == null) return@get
                val details = onIo { adminBlog.getPost(call.parameters["id"].orEmpty()) }
                if (details == null) call.respond(HttpStatusCode.NotFound, ErrorResponse("post_not_found", "Post was not found"))
                else call.respond(details)
            }
            post("/admin/posts") {
                val session = call.requireAdmin(adminAuth) ?: return@post
                if (!call.validCsrf("admin_csrf")) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("csrf_failed", "Request could not be verified"))
                    return@post
                }
                val post = call.readJson<AdminPostRequest>()
                val id = onIo { adminBlog.createDraft(session.subject, post) }
                if (id == null) call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_post", "Post details are invalid"))
                else call.respond(HttpStatusCode.Created, mapOf("id" to id, "status" to "draft"))
            }
            put("/admin/posts/{id}") {
                if (call.requireAdmin(adminAuth) == null) return@put
                if (!call.validCsrf("admin_csrf")) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("csrf_failed", "Request could not be verified"))
                    return@put
                }
                val post = call.readJson<AdminPostRequest>()
                val updated = onIo { adminBlog.updatePost(call.parameters["id"].orEmpty(), post) }
                if (!updated) call.respond(HttpStatusCode.BadRequest, ErrorResponse("post_update_failed", "Post was not found or the content is invalid"))
                else {
                    onIo { cache.invalidatePosts() }
                    call.respond(HttpStatusCode.NoContent)
                }
            }
            post("/admin/posts/{id}/publish") {
                if (call.requireAdmin(adminAuth) == null) return@post
                if (!call.validCsrf("admin_csrf")) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("csrf_failed", "Request could not be verified"))
                    return@post
                }
                if (!onIo { adminBlog.publish(call.parameters["id"].orEmpty()) }) call.respond(HttpStatusCode.Conflict, ErrorResponse("publish_failed", "Post was not found or is already published"))
                else {
                    onIo { cache.invalidatePosts() }
                    call.respond(HttpStatusCode.NoContent)
                }
            }
            post("/admin/posts/delete") {
                if (call.requireAdmin(adminAuth) == null) return@post
                if (!call.validCsrf("admin_csrf")) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("csrf_failed", "Request could not be verified"))
                    return@post
                }
                val ids = call.readJson<List<String>>()
                val deleted = onIo { adminBlog.deletePosts(ids) }
                if (deleted > 0) onIo { cache.invalidatePosts() }
                call.respond(mapOf("deleted" to deleted))
            }
            get("/admin/comments/pending") {
                if (call.requireAdmin(adminAuth) == null) return@get
                call.respond(onIo { adminBlog.pendingComments() })
            }
            post("/admin/comments/{id}/moderation") {
                if (call.requireAdmin(adminAuth) == null) return@post
                if (!call.validCsrf("admin_csrf")) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("csrf_failed", "Request could not be verified"))
                    return@post
                }
                val request = call.readJson<CommentModerationRequest>()
                if (!onIo { adminBlog.moderateComment(call.parameters["id"].orEmpty(), request.decision) }) call.respond(HttpStatusCode.BadRequest, ErrorResponse("moderation_failed", "Comment was not pending or the decision is invalid"))
                else call.respond(HttpStatusCode.NoContent)
            }
            post("/admin/posts/{id}/tars-review") {
                val session = call.requireAdmin(adminAuth) ?: return@post
                if (!call.validCsrf("admin_csrf")) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("csrf_failed", "Request could not be verified"))
                    return@post
                }
                val key = call.request.headers["Idempotency-Key"].orEmpty()
                val result = onIo { tars.enqueue(call.parameters["id"].orEmpty(), session.subject, key) }
                if (result == null) call.respond(HttpStatusCode.BadRequest, ErrorResponse("review_request_failed", "Draft was not found or idempotency key is invalid"))
                else call.respond(HttpStatusCode.Accepted, result)
            }
            get("/admin/tars/{taskId}") {
                if (call.requireAdmin(adminAuth) == null) return@get
                val task = onIo { tars.details(call.parameters["taskId"].orEmpty()) }
                if (task == null) call.respond(HttpStatusCode.NotFound, ErrorResponse("task_not_found", "Review task was not found"))
                else call.respond(task)
            }
            post("/admin/tars/{taskId}/save-draft") {
                val session = call.requireAdmin(adminAuth) ?: return@post
                if (!call.validCsrf("admin_csrf")) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("csrf_failed", "Request could not be verified"))
                    return@post
                }
                val key = call.request.headers["Idempotency-Key"].orEmpty()
                val draft = onIo { tars.saveDraft(call.parameters["taskId"].orEmpty(), session.subject, key) }
                if (draft == null) call.respond(HttpStatusCode.Conflict, ErrorResponse("draft_save_failed", "Review task has no approved revision to save"))
                else call.respond(HttpStatusCode.Created, draft)
            }
            post("/connector/tars/tasks/claim") {
                if (!call.validConnectorToken()) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("connector_unauthorized", "Connector credentials are invalid"))
                    return@post
                }
                call.respond(onIo { tars.claim() })
            }
            post("/connector/tars/tasks/result") {
                if (!call.validConnectorToken()) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("connector_unauthorized", "Connector credentials are invalid"))
                    return@post
                }
                val result = call.readJson<br.com.jaquesprojetos.blog.contracts.TarsTaskResult>()
                val accepted = onIo { tars.complete(result) }
                if (!accepted) call.respond(HttpStatusCode.Conflict, ErrorResponse("task_result_rejected", "Task lease expired or result was invalid"))
                else call.respond(HttpStatusCode.Accepted, MessageResponse("Result accepted."))
            }

            post("/auth/register") {
                val request = call.readJson<ReaderRegistrationRequest>()
                val email = request.email.trim()
                if (email.length > 254 || !email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) || request.password.length !in 12..128) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_registration", "Use a valid email and a password of 12 to 128 characters"))
                } else if (!emailRateAllowed(call, cache, email, "register", 5, 3_600)) {
                    call.respond(HttpStatusCode.TooManyRequests, ErrorResponse("rate_limited", "Too many requests"))
                } else {
                    runCatching { onIo { readers.register(email, request.password) } }
                        .onFailure { appLog.error("Reader registration failed", it) }
                    call.respond(HttpStatusCode.Accepted, MessageResponse("If the address can be registered, a verification email will be sent."))
                }
            }
            post("/auth/verify-email") {
                val request = call.readJson<VerifyEmailRequest>()
                if (request.token.length > 256 || !onIo { readers.verifyEmail(request.token) }) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_token", "The verification link is invalid or expired"))
                } else call.respond(MessageResponse("Email verified. You can now sign in."))
            }
            post("/auth/verify-email/request") {
                val request = call.readJson<EmailRequest>()
                if (request.email.length <= 254 && emailRateAllowed(call, cache, request.email, "verify-email", 5, 3_600)) {
                    runCatching { onIo { readers.requestVerification(request.email) } }
                        .onFailure { appLog.error("Verification email request failed", it) }
                }
                call.respond(HttpStatusCode.Accepted, MessageResponse("If the address needs verification, an email will be sent."))
            }
            post("/auth/login") {
                val request = call.readJson<ReaderLoginRequest>()
                val session = if (request.email.length <= 254 && request.password.length in 12..128) {
                    if (!emailRateAllowed(call, cache, request.email, "login", 5, 60)) null
                    else runCatching { onIo { readers.login(request.email, request.password) } }.getOrNull()
                } else null
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("invalid_credentials", "Email or password is incorrect, or the email is not verified"))
                } else {
                    val csrf = UUID.randomUUID().toString() + UUID.randomUUID().toString()
                    call.response.cookies.append(Cookie("reader_session", session.token, maxAge = 60 * 60 * 24 * 30, secure = secureCookies, httpOnly = true, extensions = mapOf("SameSite" to "Strict")))
                    call.response.cookies.append(Cookie("reader_csrf", csrf, maxAge = 60 * 60 * 24 * 30, secure = secureCookies, httpOnly = false, extensions = mapOf("SameSite" to "Strict")))
                    call.respond(br.com.jaquesprojetos.blog.contracts.ReaderSessionResponse(session.id.toString(), session.email))
                }
            }
            post("/auth/logout") {
                if (!call.validCsrf()) call.respond(HttpStatusCode.Forbidden, ErrorResponse("csrf_failed", "Request could not be verified"))
                else {
                    onIo { readers.logout(call.request.cookies["reader_session"]) }
                    call.response.cookies.append(Cookie("reader_session", "", maxAge = 0, secure = secureCookies, httpOnly = true, extensions = mapOf("SameSite" to "Strict")))
                    call.response.cookies.append(Cookie("reader_csrf", "", maxAge = 0, secure = secureCookies, httpOnly = false, extensions = mapOf("SameSite" to "Strict")))
                    call.respond(MessageResponse("Signed out."))
                }
            }
            post("/auth/password-reset/request") {
                val request = call.readJson<PasswordResetRequest>()
                if (request.email.length <= 254 && emailRateAllowed(call, cache, request.email, "password-reset", 5, 3_600)) {
                    runCatching { onIo { readers.requestPasswordReset(request.email) } }
                        .onFailure { appLog.error("Password reset request failed", it) }
                }
                call.respond(HttpStatusCode.Accepted, MessageResponse("If the address has an account, a reset email will be sent."))
            }
            post("/auth/password-reset/complete") {
                val request = call.readJson<PasswordResetCompleteRequest>()
                if (request.token.length > 256 || request.password.length !in 12..128 || !onIo { readers.resetPassword(request.token, request.password) }) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_reset", "The reset link is invalid or expired"))
                } else call.respond(MessageResponse("Password reset. Sign in again."))
            }
            put("/favorites/{postId}") {
                val session = onIo { readers.account(call.request.cookies["reader_session"]) }
                if (session == null) call.respond(HttpStatusCode.Unauthorized, ErrorResponse("authentication_required", "Sign in with a verified reader account"))
                else if (!call.validCsrf()) call.respond(HttpStatusCode.Forbidden, ErrorResponse("csrf_failed", "Request could not be verified"))
                else if (!onIo { readers.addFavorite(session.id, call.parameters["postId"].orEmpty()) }) call.respond(HttpStatusCode.NotFound, ErrorResponse("post_not_found", "Published post was not found"))
                else call.respond(HttpStatusCode.NoContent)
            }
            delete("/favorites/{postId}") {
                val session = onIo { readers.account(call.request.cookies["reader_session"]) }
                if (session == null) call.respond(HttpStatusCode.Unauthorized, ErrorResponse("authentication_required", "Sign in with a verified reader account"))
                else if (!call.validCsrf()) call.respond(HttpStatusCode.Forbidden, ErrorResponse("csrf_failed", "Request could not be verified"))
                else {
                    onIo { readers.removeFavorite(session.id, call.parameters["postId"].orEmpty()) }
                    call.respond(HttpStatusCode.NoContent)
                }
            }
            get("/favorites") {
                val session = onIo { readers.account(call.request.cookies["reader_session"]) }
                if (session == null) call.respond(HttpStatusCode.Unauthorized, ErrorResponse("authentication_required", "Sign in with a verified reader account"))
                else call.respond(onIo { readers.favoriteIds(session.id) })
            }
            get("/posts/{postId}/comments") {
                call.respond(onIo { readers.approvedComments(call.parameters["postId"].orEmpty()) })
            }
            post("/posts/{postId}/comments") {
                val session = onIo { readers.account(call.request.cookies["reader_session"]) }
                if (session == null) call.respond(HttpStatusCode.Unauthorized, ErrorResponse("authentication_required", "Sign in with a verified reader account"))
                else if (!call.validCsrf()) call.respond(HttpStatusCode.Forbidden, ErrorResponse("csrf_failed", "Request could not be verified"))
                else if ((onIo { cache.increment("rate:comment:${session.id}", 60L) } ?: 0L) > 10L) call.respond(HttpStatusCode.TooManyRequests, ErrorResponse("rate_limited", "Too many requests"))
                else {
                    val comment = call.readJson<br.com.jaquesprojetos.blog.contracts.CommentRequest>()
                    val created = onIo { readers.addComment(session.id, call.parameters["postId"].orEmpty(), comment.body) }
                    if (created == null) call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_comment", "Comment must be 1 to 5000 characters and reference a published post"))
                    else call.respond(HttpStatusCode.Created, created)
                }
            }

            get("/posts") {
                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceIn(0, 10_000) ?: 0
                val category = call.request.queryParameters["category"]?.takeIf { it.matches(Regex("[A-Za-z]{1,40}")) }
                val search = call.request.queryParameters["q"]?.trim()?.take(120)?.takeIf(String::isNotBlank)
                val section = call.request.queryParameters["section"]?.takeIf { it in setOf("latest", "main", "popular", "sponsored") } ?: "latest"
                val version = onIo { cache.postsVersion() }
                val cacheKey = "blog:posts:$version:$section:${category.orEmpty()}:${search.orEmpty()}:$page"
                val fresh = onIo { cache.read(cacheKey) }
                if (fresh != null) {
                    call.respond(Json.decodeFromString<PostListResponse>(fresh))
                } else {
                    try {
                        val response = PostListResponse(onIo { database.publicPosts(section, category, search, page) })
                        onIo { cache.write(cacheKey, Json.encodeToString(PostListResponse.serializer(), response)) }
                        call.respond(response)
                    } catch (failure: Exception) {
                        val stale = onIo { cache.stale(cacheKey) }
                        if (stale != null) call.respond(Json.decodeFromString<PostListResponse>(stale))
                        else call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse("temporarily_unavailable", "Posts are temporarily unavailable"))
                    }
                }
            }
            get("/posts/{id}") {
                val id = call.parameters["id"]
                val details = id?.let { postId -> onIo { database.publicPost(postId) } }
                if (details == null) call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Post was not found"))
                else call.respond(PostDetailsResponse(details))
            }
            post("/newsletter") {
                val email = Json.decodeFromString<NewsletterSubscriptionRequest>(call.receiveText()).email.trim().lowercase()
                if (email.length > 254 || !email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_email", "Enter a valid email address"))
                } else {
                    onIo { database.subscribe(email) }
                    call.respond(MessageResponse("Thanks for subscribing."))
                }
            }
        }
    }
}

internal fun Route.adminOidcCallback(
    adminAuth: AutheliaOidc,
    secureCookies: Boolean,
    webBaseUrl: String?,
    onFailure: (stage: String, failureType: String) -> Unit = { _, _ -> },
) {
    get("/admin/callback") {
        val state = call.request.queryParameters["state"].orEmpty()
        val code = call.request.queryParameters["code"].orEmpty()
        val session = if (state.length <= 256 && code.length <= 4096) {
            try {
                onIo { adminAuth.complete(state, code) }
            } catch (failure: Exception) {
                val stage = (failure as? OidcCallbackFailure)?.stage ?: "unexpected"
                val failureType = failure::class.simpleName ?: "Exception"
                onFailure(stage, failureType)
                null
            }
        } else null
        if (session == null) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("oidc_login_failed", "Could not complete the Authelia sign-in"))
        } else {
            val csrf = UUID.randomUUID().toString() + UUID.randomUUID().toString()
            call.response.cookies.append(Cookie("admin_session", session.token, maxAge = 60 * 60 * 8, secure = secureCookies, httpOnly = true, extensions = mapOf("SameSite" to "Lax")))
            call.response.cookies.append(Cookie("admin_csrf", csrf, maxAge = 60 * 60 * 8, secure = secureCookies, httpOnly = false, extensions = mapOf("SameSite" to "Lax")))
            call.respondRedirect(webBaseUrl?.trimEnd('/')?.plus("/admin") ?: "http://localhost:8080/admin")
        }
    }
}

private suspend inline fun <reified T> ApplicationCall.readJson(): T = Json.decodeFromString(receiveText())

private suspend fun <T> onIo(block: () -> T): T = withContext(Dispatchers.IO) { block() }

private fun ApplicationCall.validCsrf(cookieName: String = "reader_csrf"): Boolean {
    val cookie = request.cookies[cookieName] ?: return false
    val header = request.headers["X-CSRF-Token"] ?: return false
    return csrfTokensMatch(cookie, header)
}

internal fun csrfTokensMatch(cookie: String?, header: String?): Boolean {
    if (cookie == null || header == null) return false
    return MessageDigest.isEqual(cookie.toByteArray(), header.toByteArray())
}

private suspend fun ApplicationCall.requireAdmin(auth: AutheliaOidc): AutheliaOidc.AdminSession? {
    val session = onIo { auth.session(request.cookies["admin_session"]) }
    if (session == null) respond(HttpStatusCode.Unauthorized, ErrorResponse("authentication_required", "Sign in with Authelia"))
    return session
}

private fun ApplicationCall.validConnectorToken(): Boolean {
    val expected = System.getenv("TARS_CONNECTOR_TOKEN") ?: return false
    val actual = request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ") ?: return false
    return MessageDigest.isEqual(expected.toByteArray(), actual.toByteArray())
}

private suspend fun emailRateAllowed(call: ApplicationCall, cache: Cache, email: String, action: String, limit: Long, windowSeconds: Long): Boolean {
    val remoteHash = sha256Hex(call.request.local.remoteHost)
    val emailHash = sha256Hex(email.trim().lowercase())
    val count = onIo { cache.increment("rate:$action:$remoteHash:$emailHash", windowSeconds) }
    return count == null || count <= limit
}

private fun sha256Hex(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray())
    .take(12)
    .joinToString("") { "%02x".format(it.toInt() and 0xff) }

private fun Database.subscribe(email: String) = query { connection ->
    connection.prepareStatement("INSERT INTO newsletter_subscribers(email) VALUES (?) ON CONFLICT (email) DO NOTHING").use { statement ->
        statement.setString(1, email)
        statement.executeUpdate()
    }
}

private fun Database.publicPosts(section: String, category: String?, search: String?, page: Int): List<PostSummary> = query { connection ->
    val clauses = mutableListOf("is_published = true")
    when (section) {
        "main" -> clauses += "is_main = true"
        "popular" -> clauses += "is_popular = true"
        "sponsored" -> clauses += "is_sponsored = true"
        else -> clauses += "is_main = false AND is_popular = false AND is_sponsored = false"
    }
    if (category != null) clauses += "category = ?"
    if (search != null) clauses += "title ILIKE ?"
    val order = "created_at DESC"
    val sql = "SELECT id, author, title, subtitle, extract(epoch from created_at) * 1000 AS date, thumbnail, category, is_popular, is_main, is_sponsored FROM posts WHERE ${clauses.joinToString(" AND ")} ORDER BY $order LIMIT 9 OFFSET ?"
    connection.prepareStatement(sql).use { statement ->
        var index = 1
        if (category != null) statement.setString(index++, category)
        if (search != null) statement.setString(index++, "%$search%")
        statement.setInt(index, page * 9)
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

private fun Database.publicPost(rawId: String): PostDetails? {
    val id = runCatching { java.util.UUID.fromString(rawId) }.getOrNull() ?: return null
    return query { connection ->
        connection.prepareStatement("SELECT id, author, title, subtitle, extract(epoch from created_at) * 1000 AS date, thumbnail, category, is_popular, is_main, is_sponsored, content FROM posts WHERE id = ? AND is_published = true").use { statement ->
            statement.setObject(1, id)
            statement.executeQuery().use { rows ->
                if (!rows.next()) return@query null
                val summary = PostSummary(
                    id = rows.getString("id"), author = rows.getString("author"),
                    title = rows.getString("title"), subtitle = rows.getString("subtitle"),
                    date = rows.getDouble("date").toLong(), thumbnail = legacyGhostUrl(rows.getString("thumbnail")),
                    category = rows.getString("category"), popular = rows.getBoolean("is_popular"),
                    main = rows.getBoolean("is_main"), sponsored = rows.getBoolean("is_sponsored"),
                )
                PostDetails(
                    id = summary.id,
                    author = summary.author,
                    title = summary.title,
                    subtitle = summary.subtitle,
                    date = summary.date,
                    thumbnail = summary.thumbnail,
                    content = legacyGhostUrl(rows.getString("content")),
                    category = summary.category,
                    popular = summary.popular,
                    main = summary.main,
                    sponsored = summary.sponsored,
                )
            }
        }
    }
}

internal fun legacyGhostUrl(value: String): String {
    val base = System.getenv("LEGACY_GHOST_BASE_URL")
        ?.trimEnd('/')
        ?: "https://jaquesprojetos.com.br/convergencia"
    return value.replace("__GHOST_URL__", base)
}
