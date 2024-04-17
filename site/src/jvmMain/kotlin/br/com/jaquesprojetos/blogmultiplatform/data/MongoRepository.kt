package br.com.jaquesprojetos.blogmultiplatform.data

import br.com.jaquesprojetos.blogmultiplatform.models.Post
import br.com.jaquesprojetos.blogmultiplatform.models.User

interface MongoRepository {
   suspend fun addPost(post: Post): Boolean
   suspend fun checkUserExistence(user: User): User?
   suspend fun checkUserId(id: String): Boolean
}