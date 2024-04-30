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
import kotlinx.browser.localStorage
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.px
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.get
import org.w3c.dom.set
import kotlin.js.Date

@Composable
fun isUserLoggedIn(content: @Composable () -> Unit) {
    val context = rememberPageContext()
    val remembered = remember { localStorage["remember"].toBoolean() }
    val userId = remember { localStorage["userId"] }
    var userIdExists by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        ->
        userIdExists = if (!userId.isNullOrEmpty()) checkUserId(id = userId) else false
        if (!remembered || !userIdExists) {
            // Ensure the router is set up correctly
            context.router.navigateTo(Screen.AdminLogin.route)
        }
    }

    if (remembered && userIdExists) {
        content()
    } else {
        println("Loading...")
    }
}

fun logout() {
    localStorage["remember"] = "false"
    localStorage.removeItem("remember")
    localStorage.removeItem("userId")
    localStorage.removeItem("username")

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