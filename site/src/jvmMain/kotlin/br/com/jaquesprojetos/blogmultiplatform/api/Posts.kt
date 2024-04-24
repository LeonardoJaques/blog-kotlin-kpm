package br.com.jaquesprojetos.blogmultiplatform.api

import br.com.jaquesprojetos.blogmultiplatform.data.MongoDB
import br.com.jaquesprojetos.blogmultiplatform.models.ApiListResponse
import br.com.jaquesprojetos.blogmultiplatform.models.ApiResponse
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.AUTHOR_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.POST_ID_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.QUERY_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.SKIP_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Post
import com.varabyte.kobweb.api.Api
import com.varabyte.kobweb.api.ApiContext
import com.varabyte.kobweb.api.data.getValue
import com.varabyte.kobweb.api.http.Request
import com.varabyte.kobweb.api.http.Response
import com.varabyte.kobweb.api.http.setBodyText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.codecs.ObjectIdGenerator

@Api(routeOverride = "addpost")
suspend fun addPost(context: ApiContext) {
    try {
        val post = context.req.getBody<Post>()
        val newPost = post?.copy(
            _id = ObjectIdGenerator()
                .generate()
                .toString()
        )
        context.res.setBody(
            newPost?.let {
                context
                    .data
                    .getValue<MongoDB>()
                    .addPost(it)
            })
    } catch (e: Exception) {
        context.res.setBody(e.message)
    }
}

@Api(routeOverride = "updatepost")
suspend fun updatePost(context: ApiContext) {
    try {
        val updatePost = context.req.getBody<Post>()
        context.res.setBody(
            updatePost?.let {
                context
                    .data
                    .getValue<MongoDB>()
                    .updatePost(it)
            }
        )
    } catch (e: Exception) {
        context.res.setBody(e.message)
    }
}


@Api(routeOverride = "readmainposts")
suspend fun readMainPosts(context: ApiContext) {
    try {
        val mainPosts = context.data.getValue<MongoDB>().readMainPosts()
        context.res.setBody(ApiListResponse.Success(data = mainPosts))
    } catch (e: Exception) {
        context.res.setBody(ApiListResponse.Error(message = e.message.toString()))
    }
}

@Api(routeOverride = "readmyposts")
suspend fun readMyPosts(context: ApiContext) {
    try {
        val skip = context.req.params[SKIP_PARAM]?.toInt() ?: 0
        val author = context.req.params[AUTHOR_PARAM] ?: ""
        val result = context.data.getValue<MongoDB>().readMyPosts(skip = skip, author = author)
        context.res.setBody(ApiListResponse.Success(data = result))

    } catch (e: Exception) {
        context.res.setBody(
            ApiListResponse.Error(message = e.message.toString())
        )
    }
}


@Api(routeOverride = "searchpostbytitle")
suspend fun searchPostByTitle(context: ApiContext) {
    try {
        val query = context.req.params[QUERY_PARAM] ?: ""
        val skip = context.req.params[SKIP_PARAM]?.toInt() ?: 0
        val request = context.data.getValue<MongoDB>()
            .searchPostByTitle(
                query = query,
                skip = skip
            )
        context.res.setBody(ApiListResponse.Success(data = request))
    } catch (e: Exception) {
        context.res.setBody(ApiListResponse.Error(message = e.message.toString()))
    }
}

@Api(routeOverride = "deleteselectedposts")
suspend fun deleteSelectedPosts(context: ApiContext) = try {
    val request = context.req.getBody<List<String>>()
    context.res.setBody(
        request?.let {
            context
                .data
                .getValue<MongoDB>()
                .deleteSelectedPost(ids = it)
                .toString()
        }
    )
} catch (e: Exception) {
    context.res.setBody(e.message)
}

@Api(routeOverride = "readselectedpost")
suspend fun readSelectedPost(context: ApiContext) {
    val postId = context.req.params[POST_ID_PARAM]
    if (!postId.isNullOrEmpty()) {
        try {
            val selectedPost = context.data.getValue<MongoDB>().readSelectedPost(id = postId)
            context.res.setBody(
                ApiResponse.Success(data = selectedPost)
            )
        } catch (e: Exception) {
            context.res.setBody(
                ApiResponse.Error(message = e.message.toString())
            )
        }
    } else {
        context.res.setBody(ApiResponse.Error(message = "Post not found"))
    }
}

inline fun <reified T> Response.setBody(data: T) {
    setBodyText(Json.encodeToString<T>(data))
}

inline fun <reified T> Request.getBody(): T? {
    return body?.decodeToString()?.let {
        return Json.decodeFromString(it)
    }
}