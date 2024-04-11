package br.com.jaquesprojetos.blogmultiplatform.api


import br.com.jaquesprojetos.blogmultiplatform.data.MongoDB
import br.com.jaquesprojetos.blogmultiplatform.models.User
import br.com.jaquesprojetos.blogmultiplatform.models.UserWithoutPassword
import com.varabyte.kobweb.api.Api
import com.varabyte.kobweb.api.ApiContext
import com.varabyte.kobweb.api.data.getValue
import com.varabyte.kobweb.api.http.setBodyText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Api("usercheck")
suspend fun userCheck(context: ApiContext) {
    try {
        val userRequest =
            context.req.body?.decodeToString()?.let {
                Json.decodeFromString<User>(it)
            }
        val user = userRequest?.let {
            context.data.getValue<MongoDB>().checkUserExistence(
                User(
                    username = it.username, password = hashPassword(it.password)
                )
            )
        }

        if (user != null) {
            context.res.status = 200
            context.res.setBodyText(
                Json.encodeToString<UserWithoutPassword>(
                    UserWithoutPassword(_id = user._id, username = user.username)
                )
            )

        } else {
            context.res.status = 404
            context.res.setBodyText(
                Json.encodeToString(
                    Exception("User doesn't exit")
                )
            )
        }

    } catch (e: Exception) {
        context.res.setBodyText(
            Json.encodeToString(
                Exception(e.message)
            )
        )
    }
}

private fun hashPassword(password: String): String {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    val hasByes = messageDigest.digest(password.toByteArray(StandardCharsets.UTF_8))
    val hexString = StringBuffer()
    for (byte in hasByes) {
        hexString.append(String.format("%02x", byte))
    }
    return hexString.toString()
}