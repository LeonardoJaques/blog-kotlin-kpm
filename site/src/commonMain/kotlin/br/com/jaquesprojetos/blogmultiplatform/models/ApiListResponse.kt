package br.com.jaquesprojetos.blogmultiplatform.models

expect sealed class ApiListResponse {
    object Idle
    class Success
    class Error
}