package br.com.jaquesprojetos.blogmultiplatform.util

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await

internal data class BackendResponse(val ok: Boolean, val status: Int, val body: String)

internal fun backendBaseUrl(): String =
    if (window.location.hostname == "localhost" || window.location.hostname == "127.0.0.1") {
        "${window.location.protocol}//${window.location.hostname}:8081"
    } else "${window.location.origin}/ljblog"

@androidx.compose.runtime.NoLiveLiterals
internal suspend fun requestBackendJson(method: String, path: String, body: String, csrfCookie: String? = null, idempotencyKey: String? = null): BackendResponse {
    val init: dynamic = js("new Object()")
    init.method = method
    init.credentials = "include"
    init.headers = js("new Object()")
    init.headers["Content-Type"] = "application/json"
    init.body = body
    if (csrfCookie != null) init.headers["X-CSRF-Token"] = csrfCookie
    if (idempotencyKey != null) init.headers["Idempotency-Key"] = idempotencyKey
    val response = window.fetch("${backendBaseUrl()}/api/v1/$path", init).await()
    return BackendResponse(response.ok, response.status.toInt(), response.text().await())
}

internal suspend fun postBackendJson(path: String, body: String, csrfCookie: String? = null, idempotencyKey: String? = null): BackendResponse =
    requestBackendJson("POST", path, body, csrfCookie, idempotencyKey)

internal suspend fun putBackendJson(path: String, body: String, csrfCookie: String? = null): BackendResponse =
    requestBackendJson("PUT", path, body, csrfCookie)

internal suspend fun deleteBackend(path: String, csrfCookie: String? = null): BackendResponse {
    val init: dynamic = js("({ method: 'DELETE', credentials: 'include', headers: {} })")
    if (csrfCookie != null) init.headers["X-CSRF-Token"] = csrfCookie
    val response = window.fetch("${backendBaseUrl()}/api/v1/$path", init).await()
    return BackendResponse(response.ok, response.status.toInt(), response.text().await())
}

internal suspend fun getBackend(path: String): BackendResponse {
    val init: dynamic = js("({ method: 'GET', credentials: 'include' })")
    val response = window.fetch("${backendBaseUrl()}/api/v1/$path", init).await()
    return BackendResponse(response.ok, response.status.toInt(), response.text().await())
}

internal fun browserCookie(name: String): String? = document.cookie
    .split(';')
    .asSequence()
    .map(String::trim)
    .firstOrNull { it.startsWith("$name=") }
    ?.substringAfter('=')
