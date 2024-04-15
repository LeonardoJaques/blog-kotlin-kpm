package br.com.jaquesprojetos.blogmultiplatform.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.com.jaquesprojetos.blogmultiplatform.navigation.Screen
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.border
import com.varabyte.kobweb.compose.ui.modifiers.outline
import com.varabyte.kobweb.core.rememberPageContext
import kotlinx.browser.localStorage
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.px
import org.w3c.dom.get
import org.w3c.dom.set

@Composable
fun isUserLoggedIn( content: @Composable () -> Unit) {
    val context = rememberPageContext()
    val remembered = remember { localStorage["remember"].toBoolean() }
    val userId = remember { localStorage["userId"] }
    var userIdExists by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        ->
        userIdExists = if (!userId.isNullOrEmpty())  checkUserId(id = userId) else false
        if (!remembered || !userIdExists) {
            // Ensure the router is set up correctly
            context.router.navigateTo( Screen.AdminLogin.route)
        }
    }

    if (remembered && userIdExists) {
        content()
    } else {
        println("Loading...")
    }
}

fun  logout() {
    localStorage["remember"] = "false"
    localStorage.removeItem("remember")
    localStorage.removeItem("userId")
    localStorage.removeItem("username")

}

fun Modifier.noBorder (): Modifier{
    return this.border(
        width = 0.px,
        style = LineStyle.None,
        color = Color.transparent
    )
        .outline(
            width = 0.px,
            style = LineStyle.None,
            color = Color.transparent
        )
}
