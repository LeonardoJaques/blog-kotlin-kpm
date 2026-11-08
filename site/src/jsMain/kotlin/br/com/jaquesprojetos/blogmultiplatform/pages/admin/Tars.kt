package br.com.jaquesprojetos.blogmultiplatform.pages.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import br.com.jaquesprojetos.blog.contracts.ReviewTaskDetails
import br.com.jaquesprojetos.blog.contracts.SavedDraftResponse
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
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.silk.components.text.SpanText
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import kotlin.js.Date

@Page(routeOverride = "tars")
@Composable
fun TarsReviewPage() {
    isUserLoggedIn { TarsReviewScreen() }
}

@Composable
private fun TarsReviewScreen() {
    val context = rememberPageContext()
    val scope = rememberCoroutineScope()
    val taskId = context.route.params["taskId"].orEmpty()
    var task by remember { mutableStateOf<ReviewTaskDetails?>(null) }
    var message by remember { mutableStateOf("Carregando revisão…") }
    LaunchedEffect(taskId) {
        val response = if (taskId.isNotBlank()) runCatching { getBackend("admin/tars/$taskId") }.getOrNull() else null
        if (response?.ok == true) {
            runCatching { Json.decodeFromString<ReviewTaskDetails>(response.body) }
                .onSuccess { task = it; message = "Status da tarefa: ${it.status}" }
                .onFailure { message = "Não foi possível interpretar a revisão." }
        } else message = "A revisão não está disponível. Confira o identificador e a sessão administrativa."
    }
    AdminPageLayout {
        Column(Modifier.width(1000.px).fillMaxWidth().padding(24.px), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
            SpanText("Revisão do Tars")
            SpanText(message)
            task?.let { review ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Column(Modifier.fillMaxWidth().padding(12.px).backgroundColor(Theme.LightGray.rgb).borderRadius(6.px), verticalArrangement = Arrangement.Top) {
                        SpanText("Conteúdo original")
                        SpanText(review.originalContent)
                    }
                    Column(Modifier.fillMaxWidth().padding(12.px).backgroundColor(Theme.LightGray.rgb).borderRadius(6.px), verticalArrangement = Arrangement.Top) {
                        SpanText("Sugestão do Tars")
                        SpanText(review.revisedContent ?: "A revisão ainda não chegou.")
                    }
                }
                if (review.revisedContent != null && review.status == "review") {
                    Button(attrs = Modifier.margin(top = 8.px).padding(12.px).backgroundColor(Theme.Primary.rgb).color(Theme.White.rgb).borderRadius(4.px).onClick {
                        scope.launch {
                            val key = "save-${review.taskId}-${Date.now().toLong()}"
                            val result = runCatching {
                                postBackendJson("admin/tars/${review.taskId}/save-draft", "{}", browserCookie("admin_csrf"), key)
                            }.getOrNull()
                            if (result?.ok == true) {
                                val saved = runCatching { Json.decodeFromString<SavedDraftResponse>(result.body) }.getOrNull()
                                message = if (saved != null) "Rascunho salvo (${saved.postId}). Publique-o explicitamente na tela de edição." else "Rascunho salvo."
                            } else message = "Não foi possível salvar o rascunho. Atualize a tarefa e tente novamente."
                        }
                    }.toAttrs()) { SpanText("Salvar revisão como rascunho") }
                }
            }
        }
    }
}
