package br.com.jaquesprojetos.blogmultiplatform.util

import br.com.jaquesprojetos.blogmultiplatform.models.ApiListResponse
import br.com.jaquesprojetos.blogmultiplatform.models.ApiResponse
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.AUTHOR_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.POST_ID_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.QUERY_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.SKIP_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Post
import br.com.jaquesprojetos.blogmultiplatform.models.RandomJoke
import br.com.jaquesprojetos.blogmultiplatform.models.User
import br.com.jaquesprojetos.blogmultiplatform.models.UserWithoutPassword
import com.varabyte.kobweb.browser.api
import com.varabyte.kobweb.browser.http.http
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.get
import org.w3c.dom.set
import kotlin.js.Date


suspend fun checkUserExistence(user: User): UserWithoutPassword? {
    return try {
        window.api.tryPost(
            apiPath = "usercheck",
            body = Json.encodeToString(user).encodeToByteArray()
        )?.decodeToString().parseData()
    } catch (e: Exception) {
        println("CURRENT_USER")
        println(e.message)
        null
    }
}

suspend fun checkUserId(id: String): Boolean {
    val result = window.api.tryPost(
        apiPath = "checkuserid",
        body = Json.encodeToString(id).encodeToByteArray()
    )?.decodeToString().parseData<Boolean>()

    return try {
        result
    } catch (e: Exception) {
        println("CURRENT_USER")
        println(e.message)
        false
    }
}

suspend fun fetchRandomJoke(onComplete: (RandomJoke) -> Unit) {
    val date = localStorage["date"]
    if (date != null) {
        val diff = Date.now() - date.toDouble()
        val dayHasPassed = diff >= 86400000
        if (dayHasPassed) {

            try {
                val result = window.http.get(Constants.HUMOR_API_URL).decodeToString()
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
            val result = window.http.get(Constants.HUMOR_API_URL).decodeToString()
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
    return try {
        window.api.tryPost(
            apiPath = "addpost",
            body = Json.encodeToString(post).encodeToByteArray()
        )?.decodeToString().parseData<Boolean>()
    } catch (e: Exception) {
        println("Error: ${e.message}")
        false
    }

}

suspend fun updatePost(post: Post): Boolean {
    return try {
        window.api.tryPost(
            apiPath = "updatepost",
            body = Json.encodeToString(post).encodeToByteArray()
        )?.decodeToString().toBoolean()
    } catch (e: Exception) {
        println("Error: ${e.message}")
        false
    }
}

suspend fun fetchMainPosts(
    onSuccess: (ApiListResponse) -> Unit,
    onError: (Exception) -> Unit,
){
    try {
     val result = window.api.tryGet(
            apiPath = "readmainposts"
        )?.decodeToString()
        onSuccess(result.parseData())
    } catch (e: Exception) {
        println(e)
        onError(e)
    }
}

suspend fun fetchMyPosts(
    skip: Int,
    onSuccess: (ApiListResponse) -> Unit,
    onError: (Exception) -> Unit,
) {
    return try {
        val result = window.api.tryGet(
            apiPath = "readmyposts?${SKIP_PARAM}=$skip&${AUTHOR_PARAM}=${localStorage["username"]}",
        )?.decodeToString()
        onSuccess(result.parseData())
    } catch (e: Exception) {
        onError(e)
    }
}

suspend fun deleteSelectedPosts(ids: List<String>): Boolean {
    return try {
     window.api.tryPost(
            apiPath = "deleteselectedposts",
            body = Json.encodeToString(ids).encodeToByteArray()
        )?.decodeToString()
        true
    } catch (e: Exception) {
        println(e.message)
        false
    }
}

suspend fun searchPostByTitle(
    query: String,
    skip: Int,
    onSuccess: (ApiListResponse) -> Unit,
    onError: (Exception) -> Unit,
) {
    return try {
        val result = window.api.tryGet(
            apiPath = "searchpostbytitle?${QUERY_PARAM}=$query&${SKIP_PARAM}=$skip",
        )?.decodeToString()
        onSuccess(result.parseData())
    } catch (e: Exception) {
        onError(e)
    }
}

suspend fun fetchSelectedPost(id: String): ApiResponse {
    return try {
        val result = window.api.tryGet(
            apiPath = "readselectedpost?${POST_ID_PARAM}=$id"
        )?.decodeToString()
        result?.parseData<ApiResponse>() ?: ApiResponse.Error(message = "Result is null")
    } catch (e: Exception) {
        println(e)
        ApiResponse.Error(message = e.message.toString())
    }
}

inline fun <reified T> String?.parseData(): T {
    return Json.decodeFromString(this.toString())
}