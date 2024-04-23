package br.com.jaquesprojetos.blogmultiplatform.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject


@kotlinx.serialization.Serializable(with = ApiListResponseSerializer::class)
 sealed class ApiListResponse {
    @kotlinx.serialization.Serializable
    @SerialName("idle")
     object Idle : ApiListResponse()
    @kotlinx.serialization.Serializable
    @SerialName("success")
     data class Success (val data: List<PostWithoutDetails>) : ApiListResponse()
    @Serializable
    @SerialName("error")
     data class Error(val message: String) : ApiListResponse()
}

@kotlinx.serialization.Serializable(with = ApiResponseSerializer::class)
 sealed class ApiResponse {
    @kotlinx.serialization.Serializable
    @SerialName("idle")
     object Idle : ApiResponse()
    @kotlinx.serialization.Serializable
    @SerialName("success")
     data class Success (val data: Post) : ApiResponse()
    @kotlinx.serialization.Serializable
    @SerialName("error")
     data class Error(val message: String) : ApiResponse()
}


object ApiListResponseSerializer: JsonContentPolymorphicSerializer<ApiListResponse>(ApiListResponse::class) {
    override fun selectDeserializer(element: JsonElement)= when {
        "data" in element.jsonObject -> ApiListResponse.Success.serializer()
        "message" in element.jsonObject -> ApiListResponse.Error.serializer()
        else -> ApiListResponse.Idle.serializer()
    }

}

object ApiResponseSerializer: JsonContentPolymorphicSerializer<ApiResponse>(ApiResponse::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        "data" in element.jsonObject -> ApiResponse.Success.serializer()
        "message" in element.jsonObject -> ApiResponse.Error.serializer()
        else -> ApiResponse.Idle.serializer()
    }
}