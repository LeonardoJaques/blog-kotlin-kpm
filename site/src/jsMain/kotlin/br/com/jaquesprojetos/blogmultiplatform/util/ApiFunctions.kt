package br.com.jaquesprojetos.blogmultiplatform.util

import br.com.jaquesprojetos.blogmultiplatform.models.ApiListResponse
import br.com.jaquesprojetos.blogmultiplatform.models.ApiResponse
import br.com.jaquesprojetos.blogmultiplatform.models.Category
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.AUTHOR_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.CATEGORY_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.POST_ID_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.QUERY_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.SKIP_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Newsletter
import br.com.jaquesprojetos.blogmultiplatform.models.Post
import br.com.jaquesprojetos.blogmultiplatform.models.RandomJoke
import com.varabyte.kobweb.browser.http.http
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.get
import org.w3c.dom.set
import kotlin.js.Date


suspend fun fetchRandomJoke(onComplete: (RandomJoke) -> Unit) {
    val date = localStorage["date"]
    if (date != null) {
        val diff = Date.now() - date.toDouble()
        val dayHasPassed = diff >= 86400000
        if (dayHasPassed) {

            try {
                val result = fetchText(Constants.HUMOR_API_URL)
                onComplete(Json.decodeFromString<RandomJoke>(result))
                localStorage["date"] = Date.now().toString()
                localStorage["joke"] = result
            } catch (e: Exception) {
                onComplete(RandomJoke(id = -1, joke = "Joke not found!"))
                println("Error: ${e.message}")
            }

        } else {
            try {
                localStorage["joke"]?.let { Json.decodeFromString<RandomJoke>(it) }
                    ?.let { onComplete(it) }
            } catch (e: Exception) {
                onComplete(RandomJoke(id = -1, joke = "Joke not found!"))
                println("Error: ${e.message}")

            }
        }
    } else {

        try {
            val result = fetchText(Constants.HUMOR_API_URL)
            onComplete(Json.decodeFromString<RandomJoke>(result))
            localStorage["date"] = Date.now().toString()
            localStorage["joke"] = result
        } catch (e: Exception) {
            onComplete(RandomJoke(id = -1, joke = "Joke not found!"))
            println("Error: ${e.message}")
        }
    }

}

suspend fun addPost(post: Post): Boolean {
    return runCatching {
        postBackendJson("admin/posts", Json.encodeToString(post), browserCookie("admin_csrf")).ok
    }.getOrDefault(false)

}

suspend fun updatePost(post: Post): Boolean {
    return runCatching {
        putBackendJson("admin/posts/${encodeQuery(post._id)}", Json.encodeToString(post), browserCookie("admin_csrf")).ok
    }.getOrDefault(false)
}

suspend fun fetchMainPosts(
    onSuccess: (ApiListResponse) -> Unit,
    onError: (Exception) -> Unit,
){
    try {
     val result = apiGet("posts?section=main")
        onSuccess(result.parseData())
    } catch (e: Exception) {
        println(e)
        onError(e)
    }
}

suspend fun fetchLatestPosts(
    skip: Int,
    onSuccess: (ApiListResponse) -> Unit,
    onError: (Exception) -> Unit,
){
    try {
        val result = apiGet("posts?section=latest&page=${skip / 9}")
        onSuccess(result.parseData())
    } catch (e: Exception) {
        println(e.message)
        onError(e)
    }
}

suspend fun fetchPopularPosts(
    skip: Int,
    onSuccess: (ApiListResponse) -> Unit,
    onError: (Exception) -> Unit,
){
    try {
        val result = apiGet("posts?section=popular&page=${skip / 9}")
        onSuccess(result.parseData())
    } catch (e: Exception) {
        println(e.message)
        onError(e)
    }
}

suspend fun fetchSponsoredPosts(
    onSuccess: (ApiListResponse) -> Unit,
    onError: (Exception) -> Unit,
){
    try {
        val result = apiGet("posts?section=sponsored")
        onSuccess(result.parseData())
    } catch (e: Exception) {
        println(e.message)
        onError(e)
    }
}

suspend fun fetchMyPosts(
    skip: Int,
    onSuccess: (ApiListResponse) -> Unit,
    onError: (Exception) -> Unit,
) {
    runCatching {
        val response = getBackend("admin/posts?status=draft")
        if (!response.ok) throw IllegalStateException("Admin session is required")
        onSuccess(response.body.parseData())
    }.onFailure { onError(Exception(it.message ?: "Could not load drafts")) }
}

suspend fun deleteSelectedPosts(ids: List<String>): Boolean {
    return runCatching {
        postBackendJson("admin/posts/delete", Json.encodeToString(ids), browserCookie("admin_csrf")).ok
    }.getOrDefault(false)
}

suspend fun searchPostByTitle(
    query: String,
    skip: Int,
    onSuccess: (ApiListResponse) -> Unit,
    onError: (Exception) -> Unit,
) {
    return try {
        val result = apiGet("posts?section=latest&page=${skip / 9}&q=${encodeQuery(query)}")
        onSuccess(result.parseData())
    } catch (e: Exception) {
        println(e.message)
        onError(e)
    }
}

suspend fun searchPostsByCategory(
    category: Category,
    skip: Int,
    onSuccess: (ApiListResponse) -> Unit,
    onError: (Exception) -> Unit,
) {
    return try {
        val result = apiGet("posts?section=latest&page=${skip / 9}&category=${encodeQuery(category.name)}")
        onSuccess(result.parseData())
    } catch (e: Exception) {
        println(e.message)
        onError(e)
    }
}

suspend fun fetchSelectedPost(id: String): ApiResponse {
    return try {
        val publicResponse = getBackend("posts/${encodeQuery(id)}")
        val details = if (publicResponse.ok) {
            Json.decodeFromString<br.com.jaquesprojetos.blog.contracts.PostDetailsResponse>(publicResponse.body).data
        } else {
            val adminResponse = getBackend("admin/posts/${encodeQuery(id)}")
            if (!adminResponse.ok) throw IllegalStateException("Post was not found")
            Json.decodeFromString<br.com.jaquesprojetos.blog.contracts.PostDetails>(adminResponse.body)
        }
        ApiResponse.Success(
            Post(
                _id = details.id,
                author = details.author,
                title = details.title,
                subtitle = details.subtitle,
                date = details.date,
                thumbnail = details.thumbnail,
                content = details.content,
                category = Category.valueOf(details.category),
                popular = details.popular,
                main = details.main,
                sponsored = details.sponsored,
            )
        )
    } catch (e: Exception) {
        println(e)
        ApiResponse.Error(message = e.message.toString())
    }
}

suspend fun subscribeToNewsletter(newsletter: Newsletter): String {
    return try {
        val response = postBackendJson("newsletter", Json.encodeToString(newsletter))
        if (!response.ok) "Subscription could not be completed. Please try again later."
        else Json.decodeFromString<br.com.jaquesprojetos.blog.contracts.MessageResponse>(response.body).message
    } catch (e: Exception) {
        "Subscription could not be completed. Please try again later."
    }
}

private suspend fun apiGet(path: String): String {
    val response = getBackend(path)
    if (!response.ok) throw IllegalStateException("Request failed with status ${response.status}")
    return response.body
}

private suspend fun fetchText(url: String): String {
    val response = window.fetch(url).await()
    if (!response.ok) throw IllegalStateException("Request failed with status ${response.status}")
    return response.text().await()
}

private fun encodeQuery(value: String): String = js("encodeURIComponent(value)") as String

inline fun <reified T> String?.parseData(): T {
    return Json.decodeFromString(this.toString())
}
