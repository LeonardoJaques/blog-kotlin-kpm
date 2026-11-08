package br.com.jaquesprojetos.blogmultiplatform.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.com.jaquesprojetos.blogmultiplatform.models.ControlStyle
import br.com.jaquesprojetos.blogmultiplatform.models.EditorControl
import br.com.jaquesprojetos.blogmultiplatform.navigation.Screen
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.border
import com.varabyte.kobweb.compose.ui.modifiers.outline
import com.varabyte.kobweb.core.rememberPageContext
import kotlinx.browser.document
import kotlinx.browser.window
import com.varabyte.kobweb.silk.components.text.SpanText
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.px
import org.w3c.dom.HTMLTextAreaElement
import kotlin.js.Date

@Composable
fun isUserLoggedIn(content: @Composable () -> Unit) {
    val context = rememberPageContext()
    var authorized by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        authorized = runCatching { getBackend("admin/session").ok }.getOrDefault(false)
        checked = true
        if (!authorized) context.router.navigateTo(Screen.AdminLogin.route)
    }
    if (authorized) content() else if (!checked) SpanText("Verifying admin session…")
}

fun logout() {
    val init: dynamic = js("({ method: 'POST', credentials: 'include', headers: {} })")
    browserCookie("admin_csrf")?.let { init.headers["X-CSRF-Token"] = it }
    window.fetch("${backendBaseUrl()}/api/v1/admin/logout", init)
}

fun Modifier.noBorder(): Modifier {
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

fun getEditor() = document.getElementById(Id.editor) as HTMLTextAreaElement


fun getSelectedIntRange(): IntRange? {
    val editor = getEditor()
    val start = editor.selectionStart
    val end = editor.selectionEnd
    return if (start != null && end != null) IntRange(start, (end - 1)) else null
}

fun getSelectedText(): String? {
    val range = getSelectedIntRange()
    return if (range != null) getEditor().value.substring(range) else null
}

fun applyStyle(controlStyle: ControlStyle) {
    val selectedText = getSelectedText()
    val selectedIntRange = getSelectedIntRange()
    if (selectedText != null && selectedIntRange != null) {
        getEditor().value = getEditor().value.replaceRange(
            range = selectedIntRange,
            replacement = controlStyle.style
        )
        document.getElementById(Id.editorPreview)?.innerHTML = getEditor().value
    }
}

fun applyControlStyle(
    editorControl: EditorControl,
    onLinkClick: () -> Unit,
    onImageClick: () -> Unit
) {
    when (editorControl) {
        EditorControl.Bold -> {
            applyStyle(
                ControlStyle.Bold(
                    selectedText = getSelectedText().toString()
                )
            )
        }

        EditorControl.Italic -> {
            applyStyle(
                ControlStyle.Italic(
                    selectedText = getSelectedText().toString()
                )
            )
        }

        EditorControl.Link -> {
            onLinkClick()
        }

        EditorControl.Title -> {
            applyStyle(
                ControlStyle.Title(
                    selectedText = getSelectedText().toString()
                )
            )
        }

        EditorControl.Subtitle -> {
            applyStyle(
                ControlStyle.Subtitle(
                    selectedText = getSelectedText().toString()
                )
            )
        }

        EditorControl.Quote -> {
            applyStyle(
                ControlStyle.Quote(
                    selectedText = getSelectedText().toString()
                )
            )
        }

        EditorControl.Code -> {
            applyStyle(
                ControlStyle.Code(
                    selectedText = getSelectedText().toString()
                )
            )
        }

        EditorControl.Image -> {
            onImageClick()
        }
    }
}

fun Long.parseDateString() = Date(this).toLocaleDateString()

fun parseSwitchText(post: List<String>): String {
    return if (post.size == 1) "1 post selected" else "${post.size} posts selected"
}

fun validateEmail(email: String): Boolean {
    val regex = "^[A-Za-z](.*)(@)(.+)(\\.)(.+)"
    return regex.toRegex().matches(email)
}
