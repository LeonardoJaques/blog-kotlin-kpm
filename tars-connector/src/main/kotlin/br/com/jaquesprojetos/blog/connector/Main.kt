package br.com.jaquesprojetos.blog.connector

import br.com.jaquesprojetos.blog.contracts.TarsReviewRequest
import br.com.jaquesprojetos.blog.contracts.TarsReviewResponse
import br.com.jaquesprojetos.blog.contracts.TarsTaskClaim
import br.com.jaquesprojetos.blog.contracts.TarsTaskResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.random.Random

fun main() = runBlocking {
    val api = requireEnv("BLOG_API_URL").trimEnd('/')
    val connectorToken = requireEnv("TARS_CONNECTOR_TOKEN")
    val reviewerUrl = URI.create(requireEnv("TARS_REVIEW_URL"))
    require(reviewerUrl.scheme == "https" || reviewerUrl.host in setOf("localhost", "127.0.0.1")) {
        "TARS_REVIEW_URL must use HTTPS outside localhost"
    }
    val reviewerToken = System.getenv("TARS_REVIEW_TOKEN")
    val json = Json { ignoreUnknownKeys = true }
    val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    var failures = 0
    var openUntil = 0L
    println("Tars connector started")
    while (true) {
        if (System.currentTimeMillis() < openUntil) {
            delay(2_000)
            continue
        }
        try {
            val claimsRequest = HttpRequest.newBuilder(URI.create("$api/api/v1/connector/tars/tasks/claim"))
                .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer $connectorToken")
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build()
            val claims = client.send(claimsRequest, HttpResponse.BodyHandlers.ofString())
            if (claims.statusCode() !in 200..299) error("Claim endpoint returned ${claims.statusCode()}")
            val tasks = json.decodeFromString<List<TarsTaskClaim>>(claims.body())
            for (task in tasks) {
                val result = try {
                    val reviewBody = json.encodeToString(TarsReviewRequest(task.taskId, task.content))
                    val builder = HttpRequest.newBuilder(reviewerUrl).timeout(Duration.ofSeconds(60))
                        .header("Content-Type", "application/json").header("Idempotency-Key", task.taskId)
                    if (!reviewerToken.isNullOrBlank()) builder.header("Authorization", "Bearer $reviewerToken")
                    val response = client.send(builder.POST(HttpRequest.BodyPublishers.ofString(reviewBody)).build(), HttpResponse.BodyHandlers.ofString())
                    if (response.statusCode() !in 200..299) error("Reviewer returned ${response.statusCode()}")
                    val reviewed = json.decodeFromString<TarsReviewResponse>(response.body())
                    TarsTaskResult(task.taskId, revisedContent = reviewed.revisedContent)
                } catch (_: Exception) {
                    TarsTaskResult(task.taskId, error = "Review service temporarily unavailable")
                }
                val submitted = client.send(HttpRequest.newBuilder(URI.create("$api/api/v1/connector/tars/tasks/result"))
                    .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer $connectorToken")
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(result))).build(),
                    HttpResponse.BodyHandlers.ofString())
                if (submitted.statusCode() !in 200..299) error("Result endpoint returned ${submitted.statusCode()}")
            }
            failures = 0
            delay(if (tasks.isEmpty()) 2_000 else 100)
        } catch (failure: Exception) {
            failures++
            System.err.println("Tars connector request failed (${failure.javaClass.simpleName})")
            if (failures >= 5) {
                openUntil = System.currentTimeMillis() + 30_000 + Random.nextLong(0, 5_000)
                failures = 0
            }
            delay(2_000)
        }
    }
}

private fun requireEnv(name: String): String = System.getenv(name)?.takeIf(String::isNotBlank)
    ?: error("$name is required")
