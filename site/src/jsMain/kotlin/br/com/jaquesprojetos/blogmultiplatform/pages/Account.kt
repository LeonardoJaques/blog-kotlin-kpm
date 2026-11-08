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
    var message by remember { mutableStateOf("Sign in or create an account to save favorites and comment.") }
    var signedIn by remember { mutableStateOf(false) }
    val verifyToken = window.location.search.substringAfter("verify=", "").substringBefore('&').takeIf(String::isNotBlank)
    val resetToken = window.location.search.substringAfter("reset=", "").substringBefore('&').takeIf(String::isNotBlank)
    fun value(id: String) = (document.getElementById(id) as? HTMLInputElement)?.value.orEmpty()
    fun submit(path: String, body: String, success: String) {
        scope.launch {
            val response = runCatching { postBackendJson(path, body) }.getOrNull()
            message = if (response?.ok == true) success else "We couldn't complete your request. Check your details and try again."
            if (path == "auth/login" && response?.ok == true) signedIn = true
        }
    }
    LaunchedEffect(Unit) {
        val response = runCatching { getBackend("favorites") }.getOrNull()
        if (response?.ok == true) {
            signedIn = true
            message = "You are signed in. Favorites: ${response.body}"
        }
    }
    Column(Modifier.width(640.px).fillMaxWidth().padding(24.px), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
        SpanText("Reader account")
        Input(InputType.Email, attrs = Modifier.id("reader-email").fillMaxWidth().padding(12.px).toAttrs { attr("placeholder", "Email") })
        Input(InputType.Password, attrs = Modifier.id("reader-password").fillMaxWidth().padding(12.px).toAttrs { attr("placeholder", "Password (at least 12 characters)") })
        if (resetToken != null) {
            SpanText("Set a new password to finish resetting your account.")
            Button(attrs = buttonStyle().onClick {
                submit("auth/password-reset/complete", Json.encodeToString(mapOf("token" to resetToken, "password" to value("reader-password"))), "Password updated. Please sign in again.")
            }.toAttrs()) { SpanText("Save new password") }
        } else {
            Row(horizontalArrangement = Arrangement.Start) {
                Button(attrs = buttonStyle().onClick {
                    submit("auth/register", Json.encodeToString(mapOf("email" to value("reader-email"), "password" to value("reader-password"))), "If this email can be registered, we'll send you a verification email.")
                }.toAttrs()) { SpanText("Create account") }
                Button(attrs = buttonStyle().onClick {
                    submit("auth/login", Json.encodeToString(mapOf("email" to value("reader-email"), "password" to value("reader-password"))), "You are now signed in.")
                }.toAttrs()) { SpanText("Sign in") }
            }
            Button(attrs = buttonStyle().onClick {
                submit("auth/verify-email/request", Json.encodeToString(mapOf("email" to value("reader-email"))), "If your account needs verification, we'll send you an email.")
            }.toAttrs()) { SpanText("Resend verification email") }
            Button(attrs = buttonStyle().onClick {
                submit("auth/password-reset/request", Json.encodeToString(mapOf("email" to value("reader-email"))), "If the email is registered, we'll send you instructions.")
            }.toAttrs()) { SpanText("Forgot password") }
        }
        if (verifyToken != null) Button(attrs = buttonStyle().onClick {
            submit("auth/verify-email", Json.encodeToString(mapOf("token" to verifyToken)), "Email verified. You can now sign in.")
        }.toAttrs()) { SpanText("Verify my email") }
        if (signedIn) {
            Button(attrs = buttonStyle().onClick {
                scope.launch {
                    val response = runCatching { getBackend("favorites") }.getOrNull()
                    message = if (response?.ok == true) "Favorites: ${response.body}" else "Couldn't load favorites."
                }
            }.toAttrs()) { SpanText("View favorites") }
            Button(attrs = buttonStyle().onClick {
                scope.launch {
                    val csrf = browserCookie("reader_csrf")
                    val response = runCatching { postBackendJson("auth/logout", "{}", csrf) }.getOrNull()
                    if (response?.ok == true) { signedIn = false; message = "You have signed out." }
                }
            }.toAttrs()) { SpanText("Sign out") }
        }
        SpanText(message)
    }
}

private fun buttonStyle() = Modifier.margin(top = 4.px).padding(10.px)
    .backgroundColor(Theme.Primary.rgb).color(Theme.White.rgb).borderRadius(4.px)
