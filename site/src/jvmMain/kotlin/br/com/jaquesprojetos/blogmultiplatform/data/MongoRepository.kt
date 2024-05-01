package br.com.jaquesprojetos.blogmultiplatform.data

import br.com.jaquesprojetos.blogmultiplatform.models.Category
import br.com.jaquesprojetos.blogmultiplatform.models.Newsletter
import br.com.jaquesprojetos.blogmultiplatform.models.Post
import br.com.jaquesprojetos.blogmultiplatform.models.PostWithoutDetails
import br.com.jaquesprojetos.blogmultiplatform.models.User

interface MongoRepository {
    suspend fun addPost(post: Post): Boolean
    suspend fun updatePost(post: Post): Boolean
    suspend fun readMainPosts(): List<PostWithoutDetails>
    suspend fun readSponsoredPosts(): List<PostWithoutDetails>
    suspend fun readPopularPosts(skip: Int): List<PostWithoutDetails>
    suspend fun readMyPosts(skip: Int, author: String): List<PostWithoutDetails>
    suspend fun readLatestPosts(skip: Int): List<PostWithoutDetails>
    suspend fun deleteSelectedPost(ids: List<String>): Boolean
    suspend fun readSelectedPost(id: String): Post
    suspend fun searchPostByTitle(query: String, skip: Int): List<PostWithoutDetails>
    suspend fun searchPostByCategory(category: Category, skip: Int): List<PostWithoutDetails>
    suspend fun checkUserExistence(user: User): User?

    suspend fun checkUserId(id: String): Boolean
    suspend fun subscribe(newsletter: Newsletter): String


}