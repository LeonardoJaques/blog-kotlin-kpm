package br.com.jaquesprojetos.blogmultiplatform.models

expect class Post {
    val _id: String
    val author: String
    val title: String
    val subtitle: String
    val date: Long
    val thumbnail: String
    val content: String
    val category: Category
    val popular: Boolean
    val main: Boolean
    val sponsored: Boolean

}

expect class PostWithoutDetails {
    val _id: String
    val author: String
    val title: String
    val subtitle: String
    val date: Long
    val thumbnail: String
    val category: Category

}