package br.com.jaquesprojetos.blogmultiplatform.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ReaderSessionState {
    var signedIn by mutableStateOf(false)
    var email by mutableStateOf<String?>(null)
}
