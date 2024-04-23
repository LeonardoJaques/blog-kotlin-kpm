package br.com.jaquesprojetos.blogmultiplatform.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
 data class Post(
    @SerialName("_id")
     val _id: String = "",
     val author: String ="",
     val title: String,
     val subtitle: String,
     val date: Long = 0L,
     val thumbnail: String,
     val content: String,
     val category: Category,
     val popular: Boolean = false,
     val main: Boolean = false,
     val sponsored: Boolean = false,
)
@Serializable
 data class PostWithoutDetails(
    @SerialName("_id")
     val _id: String,
     val author: String,
     val title: String,
     val subtitle: String,
     val date: Long,
     val thumbnail: String,
     val category: Category,
)

