package br.com.jaquesprojetos.blogmultiplatform.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.util.browserCookie
import br.com.jaquesprojetos.blogmultiplatform.util.getBackend
import br.com.jaquesprojetos.blogmultiplatform.util.postBackendJson
import com.varabyte.kobweb.compose.foundation.layout.Arrangement
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.id
import com.varabyte.kobweb.compose.ui.modifiers.onClick
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.text.SpanText
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Input
import org.w3c.dom.HTMLInputElement

@Page
@Composable
fun AccountPage() {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("Entre ou crie uma conta para salvar favoritos e comentar.") }
    var signedIn by remember { mutableStateOf(false) }
    val verifyToken = window.location.search.substringAfter("verify=", "").substringBefore('&').takeIf(String::isNotBlank)
    val resetToken = window.location.search.substringAfter("reset=", "").substringBefore('&').takeIf(String::isNotBlank)
    fun value(id: String) = (document.getElementById(id) as? HTMLInputElement)?.value.orEmpty()
    fun submit(path: String, body: String, success: String) {
        scope.launch {
            val response = runCatching { postBackendJson(path, body) }.getOrNull()
            message = if (response?.ok == true) success else "Não foi possível concluir. Confira os dados e tente novamente."
            if (path == "auth/login" && response?.ok == true) signedIn = true
        }
    }
    LaunchedEffect(Unit) {
        val response = runCatching { getBackend("favorites") }.getOrNull()
        if (response?.ok == true) {
            signedIn = true
            message = "Você está conectado. Favoritos: ${response.body}"
        }
    }
    Column(Modifier.width(640.px).fillMaxWidth().padding(24.px), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
        SpanText("Conta de leitor")
        Input(InputType.Email, attrs = Modifier.id("reader-email").fillMaxWidth().padding(12.px).toAttrs { attr("placeholder", "E-mail") })
        Input(InputType.Password, attrs = Modifier.id("reader-password").fillMaxWidth().padding(12.px).toAttrs { attr("placeholder", "Senha (mínimo 12 caracteres)") })
        if (resetToken != null) {
            SpanText("Defina uma nova senha para concluir a recuperação.")
            Button(attrs = buttonStyle().onClick {
                submit("auth/password-reset/complete", Json.encodeToString(mapOf("token" to resetToken, "password" to value("reader-password"))), "Senha alterada. Entre novamente.")
            }.toAttrs()) { SpanText("Salvar nova senha") }
        } else {
            Row(horizontalArrangement = Arrangement.Start) {
                Button(attrs = buttonStyle().onClick {
                    submit("auth/register", Json.encodeToString(mapOf("email" to value("reader-email"), "password" to value("reader-password"))), "Se o endereço puder ser cadastrado, enviaremos um e-mail de verificação.")
                }.toAttrs()) { SpanText("Criar conta") }
                Button(attrs = buttonStyle().onClick {
                    submit("auth/login", Json.encodeToString(mapOf("email" to value("reader-email"), "password" to value("reader-password"))), "Login realizado.")
                }.toAttrs()) { SpanText("Entrar") }
            }
            Button(attrs = buttonStyle().onClick {
                submit("auth/verify-email/request", Json.encodeToString(mapOf("email" to value("reader-email"))), "Se a conta precisar de verificação, enviaremos um e-mail.")
            }.toAttrs()) { SpanText("Reenviar verificação") }
            Button(attrs = buttonStyle().onClick {
                submit("auth/password-reset/request", Json.encodeToString(mapOf("email" to value("reader-email"))), "Se o endereço estiver cadastrado, enviaremos instruções.")
            }.toAttrs()) { SpanText("Esqueci minha senha") }
        }
        if (verifyToken != null) Button(attrs = buttonStyle().onClick {
            submit("auth/verify-email", Json.encodeToString(mapOf("token" to verifyToken)), "E-mail verificado. Agora você pode entrar.")
        }.toAttrs()) { SpanText("Confirmar meu e-mail") }
        if (signedIn) {
            Button(attrs = buttonStyle().onClick {
                scope.launch {
                    val response = runCatching { getBackend("favorites") }.getOrNull()
                    message = if (response?.ok == true) "Favoritos: ${response.body}" else "Não foi possível carregar favoritos."
                }
            }.toAttrs()) { SpanText("Ver favoritos") }
            Button(attrs = buttonStyle().onClick {
                scope.launch {
                    val csrf = browserCookie("reader_csrf")
                    val response = runCatching { postBackendJson("auth/logout", "{}", csrf) }.getOrNull()
                    if (response?.ok == true) { signedIn = false; message = "Você saiu da sua conta." }
                }
            }.toAttrs()) { SpanText("Sair") }
        }
        SpanText(message)
    }
}

private fun buttonStyle() = Modifier.margin(top = 4.px).padding(10.px)
    .backgroundColor(Theme.Primary.rgb).color(Theme.White.rgb).borderRadius(4.px)
