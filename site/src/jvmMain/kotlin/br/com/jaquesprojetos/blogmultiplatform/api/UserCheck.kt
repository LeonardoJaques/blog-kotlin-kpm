package br.com.jaquesprojetos.blogmultiplatform.api

import br.com.jaquesprojetos.blogmultiplatform.models.User
import com.varabyte.kobweb.api.Api
import com.varabyte.kobweb.api.ApiContext
import kotlinx.serialization.json.Json

@Api("usercheck")
suspend fun UserCheck(context: ApiContext ) {
    try {
     val userRequest = context.req.body?.decodeToString()?.let {
         Json.decodeFromString<User>(it)
     }
    }catch (e: Exception) {
        context.logger.error(e.message.toString())
    }
}