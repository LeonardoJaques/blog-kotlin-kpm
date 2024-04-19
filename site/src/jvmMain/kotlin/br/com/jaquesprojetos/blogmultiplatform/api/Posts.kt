package br.com.jaquesprojetos.blogmultiplatform.api

import br.com.jaquesprojetos.blogmultiplatform.data.MongoDB
import br.com.jaquesprojetos.blogmultiplatform.models.ApiListResponse
import br.com.jaquesprojetos.blogmultiplatform.models.Post
import com.varabyte.kobweb.api.Api
import com.varabyte.kobweb.api.ApiContext
import com.varabyte.kobweb.api.data.getValue
import com.varabyte.kobweb.api.http.setBodyText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.codecs.ObjectIdGenerator

@Api(routeOverride = "addpost")
suspend fun addPost(context: ApiContext) {
    try {
        val post = context.req.body?.decodeToString()?.let { Json.decodeFromString<Post>(it) }
        val newPost = post?.copy(_id = ObjectIdGenerator().generate().toString())
        context.res.setBodyText(newPost?.let {
            context.data.getValue<MongoDB>().addPost(it).toString()
        } ?: false.toString())
    } catch (e: Exception) {
        context.res.setBodyText(Json.encodeToString(e.message))
    }
}

@Api(routeOverride = "readmyposts")
suspend fun readMyPosts(context: ApiContext) {
    try {
        val skip = context.req.params["skip"]?.toInt() ?: 0
        val author = context.req.params["author"] ?: ""
        val result = context.data.getValue<MongoDB>().readMyPosts(skip = skip, author = author)
         context.res.setBodyText(
            Json.encodeToString(
                ApiListResponse.Success(data = result)
            )
         )
    } catch (e: Exception) {
        context.res.setBodyText(
            Json.encodeToString(
                ApiListResponse.Error(message = e.message.toString())
            )
        )
    }
}