package br.com.jaquesprojetos.blogmultiplatform.models

import kotlinx.serialization.SerialName
import org.bson.codecs.ObjectIdGenerator


data class User(
    @SerialName(value = "_id")
    val id: String = ObjectIdGenerator().generate().toString(),
    val userName: String,
    val password: String
)
