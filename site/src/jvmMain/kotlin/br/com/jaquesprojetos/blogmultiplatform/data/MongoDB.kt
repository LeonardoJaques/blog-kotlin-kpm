package br.com.jaquesprojetos.blogmultiplatform.data

import br.com.jaquesprojetos.blogmultiplatform.models.User
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.DATABASE_NAME
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.and
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.varabyte.kobweb.api.data.add
import com.varabyte.kobweb.api.init.InitApi
import com.varabyte.kobweb.api.init.InitApiContext
import kotlinx.coroutines.flow.firstOrNull


@InitApi
fun initMongoDB(context: InitApiContext) {
    context.data.add(MongoDB(context))
}

class MongoDB(val context: InitApiContext) : MongoRepository {
    private val client = MongoClient.create()
    private val database = client.getDatabase(DATABASE_NAME)
    private val userCollection = database.getCollection<User>("user")

    override suspend fun checkUserExistence(user: User): User? {
        return try {
            userCollection.find(
                and(
                    Filters.eq("userName", user.userName),
                    Filters.eq("password", user.password)
                )
            ).firstOrNull()
        } catch (e: Exception) {
            context.logger.error(e.message.toString())
            null
        }
    }
}




