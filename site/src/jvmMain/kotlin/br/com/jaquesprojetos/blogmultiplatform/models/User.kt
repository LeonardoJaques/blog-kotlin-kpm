package br.com.jaquesprojetos.blogmultiplatform.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.codecs.ObjectIdGenerator


@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@Serializable
actual data class User(
    @SerialName(value = "_id")
    actual val _id: String = ObjectIdGenerator().generate().toString(),
    actual val username: String,
    actual val password: String
)

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@Serializable
actual data class UserWithoutPassword(
    actual val _id: String = ObjectIdGenerator().generate().toString(),
    actual val username: String,

    )
