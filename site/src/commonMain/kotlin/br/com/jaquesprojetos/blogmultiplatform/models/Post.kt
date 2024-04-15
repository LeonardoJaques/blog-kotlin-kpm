package br.com.jaquesprojetos.blogmultiplatform.models

expect class Post {
    val id: String
    val body: String
    val author: String
    val title: String
    val subTitle: String
    val date: Long
    val thumbnail: String
    val content: String
    val category: Category
    val popular: Boolean
    val main: Boolean
    val sponsored: Boolean

}