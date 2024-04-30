package br.com.jaquesprojetos.blogmultiplatform.models

import kotlinx.serialization.Serializable

@Serializable
data class Newsletter(
    val email: String
)
