package br.com.jaquesprojetos.blogmultiplatform.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.codecs.ObjectIdGenerator

@Serializable
actual data class Post(
    @SerialName("_id")
    actual val id: String = ObjectIdGenerator().generate().toString(),
    actual val body: String,
    actual val author: String,
    actual val title: String,
    actual val subTitle: String,
    actual val date: Long,
    actual val thumbnail: String,
    actual val content: String,
    actual val category: Category,
    actual val popular: Boolean = false,
    actual val main: Boolean = false,
    actual val sponsored: Boolean= false
)