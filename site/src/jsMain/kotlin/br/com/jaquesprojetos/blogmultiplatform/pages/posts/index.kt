package br.com.jaquesprojetos.blogmultiplatform.pages.posts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import br.com.jaquesprojetos.blogmultiplatform.components.ErrorView
import br.com.jaquesprojetos.blogmultiplatform.components.LoadingIndicator
import br.com.jaquesprojetos.blogmultiplatform.components.OverflowSidePanel
import br.com.jaquesprojetos.blogmultiplatform.components.categoryNavigationItems
import br.com.jaquesprojetos.blogmultiplatform.models.ApiResponse
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.POST_ID_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Post
import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.sections.FooterSection
import br.com.jaquesprojetos.blogmultiplatform.sections.HeaderSection
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.FONT_FAMILY
import br.com.jaquesprojetos.blogmultiplatform.util.Id
import br.com.jaquesprojetos.blogmultiplatform.util.Res.Image.logo
import br.com.jaquesprojetos.blogmultiplatform.util.fetchSelectedPost
import br.com.jaquesprojetos.blogmultiplatform.util.parseDateString
import com.varabyte.kobweb.compose.css.TextOverflow
import com.varabyte.kobweb.compose.foundation.layout.Arrangement
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.fontFamily
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.id
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.maxWidth
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.textOverflow
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.theme.breakpoint.rememberBreakpoint
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement

@Page(routeOverride = "post")
@Composable
fun PostsPage() {
    val context = rememberPageContext()
    val breakpoint = rememberBreakpoint()
    val scope = rememberCoroutineScope()

    var overflowMenuOpened by remember { mutableStateOf(false) }
    var apiResponse by remember { mutableStateOf<ApiResponse>(ApiResponse.Idle) }

    val hasPostIdParam = remember(key1 = context.route) {
        context.route.params.containsKey(POST_ID_PARAM)
    }
    LaunchedEffect(key1 = context.route) {
        if (hasPostIdParam) {
            val postId = context.route.params[POST_ID_PARAM] ?: error("")
            apiResponse = fetchSelectedPost(id = postId)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (overflowMenuOpened) {
            OverflowSidePanel(
                onMenuClosed = { overflowMenuOpened = false },
                content = {
                    categoryNavigationItems(
                        onMenuOpen = { overflowMenuOpened = true },
                        vertical = true
                    )
                }
            )
        }
        HeaderSection(
            breakpoint = breakpoint,
            onMenuOpen = { overflowMenuOpened = true },
            logo = logo,
        )
        when (apiResponse) {
            is ApiResponse.Success -> {
                scope.launch {
                    delay(50)
                    try {
                        js("hljs.highlightAll()") as Unit
                    } catch (e: Exception) {
                        println("Error highlighting code blocks: $e")
                    }
                }
                PostContent(post = (apiResponse as ApiResponse.Success).data)
            }

            is ApiResponse.Idle -> LoadingIndicator()
            is ApiResponse.Error -> {
                val responseMessage = (apiResponse as ApiResponse.Error).message
                ErrorView(message = responseMessage)
            }
        }
        FooterSection()
    }
}

@Composable
fun PostContent(post: Post) {
    LaunchedEffect(post) {
        (document.getElementById(Id.postContent) as HTMLDivElement).innerHTML = post.content
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .margin(top = 50.px, bottom = 200.px)
            .padding(leftRight = 24.px)
            .maxWidth(800.px),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SpanText(
            modifier = Modifier
                .fillMaxWidth()
                .color(Theme.HalfBlack.rgb)
                .fontFamily(FONT_FAMILY)
                .fontSize(14.px),
            text = post.date.parseDateString()
        )
        SpanText(
            modifier = Modifier
                .fillMaxWidth()
                .margin(bottom = 20.px)
                .color(Theme.HalfBlack.rgb)
                .fontFamily(FONT_FAMILY)
                .fontSize(40.px)
                .textOverflow(TextOverflow.Ellipsis)
                .styleModifier {
                    property("display", "-webkit-box")
                    property("-webkit-line-clamp", "2")
                    property("line-clamp", "2")
                    property("-webkit-box-orient", "vertical")
                },
            text = post.title
        )
        Image(
            modifier = Modifier
                .fillMaxSize()
                .borderRadius(8.px)
                .margin(bottom = 40.px),

            src = post.thumbnail,
            alt = post.title
        )
        Div(
            attrs = Modifier
                .id(Id.postContent)
                .fontFamily(FONT_FAMILY)
                .fillMaxWidth()
                .toAttrs()
        ) {

        }

    }
}