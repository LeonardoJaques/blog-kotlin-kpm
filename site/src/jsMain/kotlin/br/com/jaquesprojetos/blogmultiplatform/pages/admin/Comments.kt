package br.com.jaquesprojetos.blogmultiplatform.pages.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import br.com.jaquesprojetos.blog.contracts.CommentModerationItem
import br.com.jaquesprojetos.blogmultiplatform.components.AdminPageLayout
import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.util.browserCookie
import br.com.jaquesprojetos.blogmultiplatform.util.getBackend
import br.com.jaquesprojetos.blogmultiplatform.util.isUserLoggedIn
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
import com.varabyte.kobweb.compose.ui.modifiers.onClick
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.text.SpanText
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button

@Page
@Composable
fun CommentsPage() {
    isUserLoggedIn { PendingCommentsScreen() }
}

@Composable
private fun PendingCommentsScreen() {
    val comments = remember { mutableStateListOf<CommentModerationItem>() }
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("Carregando comentários pendentes…") }
    suspend fun refresh() {
        val response = runCatching { getBackend("admin/comments/pending") }.getOrNull()
        if (response?.ok == true) {
            runCatching { Json.decodeFromString<List<CommentModerationItem>>(response.body) }
                .onSuccess { comments.clear(); comments.addAll(it); message = if (it.isEmpty()) "Não há comentários pendentes." else "" }
                .onFailure { message = "Não foi possível ler a lista de comentários." }
        } else message = "Não foi possível carregar os comentários. Confirme a sessão administrativa."
    }
    LaunchedEffect(Unit) { refresh() }
    AdminPageLayout {
        Column(
            Modifier.width(900.px).fillMaxWidth().padding(24.px),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            SpanText("Moderação de comentários")
            if (message.isNotEmpty()) SpanText(message)
            comments.forEach { comment ->
                Column(Modifier.fillMaxWidth().padding(16.px).backgroundColor(Theme.LightGray.rgb).borderRadius(6.px), verticalArrangement = Arrangement.Top) {
                    SpanText("${comment.email} · post ${comment.postId}")
                    SpanText(comment.body)
                    Row(horizontalArrangement = Arrangement.Start) {
                        Button(attrs = actionStyle().onClick {
                            scope.launch {
                                val csrf = browserCookie("admin_csrf")
                                val result = runCatching {
                                    postBackendJson("admin/comments/${comment.id}/moderation", Json.encodeToString(mapOf("decision" to "approve")), csrf)
                                }.getOrNull()
                                if (result?.ok == true) { comments.remove(comment); message = if (comments.isEmpty()) "Não há comentários pendentes." else "" }
                                else message = "A aprovação falhou; atualize a sessão e tente novamente."
                            }
                        }.toAttrs()) { SpanText("Aprovar") }
                        Button(attrs = actionStyle().onClick {
                            scope.launch {
                                val csrf = browserCookie("admin_csrf")
                                val result = runCatching {
                                    postBackendJson("admin/comments/${comment.id}/moderation", Json.encodeToString(mapOf("decision" to "reject")), csrf)
                                }.getOrNull()
                                if (result?.ok == true) { comments.remove(comment); message = if (comments.isEmpty()) "Não há comentários pendentes." else "" }
                                else message = "A rejeição falhou; atualize a sessão e tente novamente."
                            }
                        }.toAttrs()) { SpanText("Rejeitar") }
                    }
                }
            }
        }
    }
}

private fun actionStyle() = Modifier.padding(leftRight = 14.px, topBottom = 8.px)
    .backgroundColor(Theme.Primary.rgb).color(Theme.White.rgb).borderRadius(4.px)
