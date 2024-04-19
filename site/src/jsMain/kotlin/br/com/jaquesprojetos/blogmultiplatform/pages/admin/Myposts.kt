package br.com.jaquesprojetos.blogmultiplatform.pages.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import br.com.jaquesprojetos.blogmultiplatform.components.AdminPageLayout
import br.com.jaquesprojetos.blogmultiplatform.components.Posts
import br.com.jaquesprojetos.blogmultiplatform.components.SearchBar
import br.com.jaquesprojetos.blogmultiplatform.models.ApiListResponse
import br.com.jaquesprojetos.blogmultiplatform.models.PostWithoutDetails
import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.FONT_FAMILY
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.POSTS_PER_PAGE
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.SIDE_PANEL_WIDTH
import br.com.jaquesprojetos.blogmultiplatform.util.fetchMyPosts
import br.com.jaquesprojetos.blogmultiplatform.util.isUserLoggedIn
import br.com.jaquesprojetos.blogmultiplatform.util.noBorder
import br.com.jaquesprojetos.blogmultiplatform.util.parseSwitchText
import com.varabyte.kobweb.compose.foundation.layout.Arrangement
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.fontFamily
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.onClick
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.forms.Switch
import com.varabyte.kobweb.silk.components.forms.SwitchSize
import com.varabyte.kobweb.silk.components.style.breakpoint.Breakpoint
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.theme.breakpoint.rememberBreakpoint
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button

@Page
@Composable
fun MyPostsPage() {
    isUserLoggedIn { MyPostsScreen() }
}

@Composable
fun MyPostsScreen() {
    val breakpoint = rememberBreakpoint()
    val scope = rememberCoroutineScope()
    val myPosts = remember { mutableStateListOf<PostWithoutDetails>() }
    var selectable by remember { mutableStateOf(false) }
    var switchText by remember { mutableStateOf("select") }
    var shoMoreVisibility by remember { mutableStateOf(false) }
    var postToSkip by remember { mutableStateOf(0) }
    var selectedPosts = remember { mutableStateListOf<String>() }
    LaunchedEffect(Unit) {
        fetchMyPosts(
            skip = postToSkip,
            onSuccess = {
                if (it is ApiListResponse.Success) {
                    myPosts.clear()
                    myPosts.addAll(it.data)
                    postToSkip += POSTS_PER_PAGE
                    shoMoreVisibility = it.data.size >= POSTS_PER_PAGE
                }
            },
            onError = { println(it) }
        )
    }
    AdminPageLayout {
        Column(
            modifier = Modifier
                .margin(topBottom = 50.px)
                .fillMaxSize()
                .padding(
                    left = if (breakpoint > Breakpoint.MD) SIDE_PANEL_WIDTH.px else 0.px
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .margin(bottom = 24.px)
                    .fillMaxWidth(
                        if (breakpoint > Breakpoint.MD) 30.percent
                        else 50.percent
                    ),
                contentAlignment = Alignment.Center
            ) {
                SearchBar(
                    onEnterClick = {}
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(if (breakpoint > Breakpoint.MD) 80.percent else 90.percent)
                    .margin(bottom = 24.px),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.margin(right = 8.px),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        size = SwitchSize.SM,
                        checked = selectable,
                        onCheckedChange = {
                            selectable = it
                            if (!selectable) {
                                switchText = "select"
                                selectedPosts.clear()
                            } else {
                                switchText = "0 posts selected"
                            }
                        }
                    )
                    SpanText(
                        modifier = Modifier
                            .margin(left = 8.px)
                            .color(
                                if (selectable) Theme.Black.rgb
                                else Theme.HalfBlack.rgb
                            ),
                        text = switchText
                    )

                }
                Button(
                    attrs = Modifier
                        .padding(leftRight = 24.px)
                        .backgroundColor(Theme.Red.rgb)
                        .margin(right = 20.px)
                        .noBorder()
                        .borderRadius(4.px)
                        .color(Theme.White.rgb)
                        .fontFamily(FONT_FAMILY)
                        .fontSize(14.px)
                        .height(54.px)
                        .onClick { }
                        .toAttrs()
                ) {
                    SpanText(text = "Delete")
                }
            }
            Posts(
                post = myPosts,
                breakpoint = breakpoint,
                showMoreVisibility = shoMoreVisibility,
                selectable = selectable,
                onSelect = {
                    selectedPosts.add(it)
                    switchText = parseSwitchText(selectedPosts)
                },
                onDeselect = {
                    selectedPosts.remove(it)
                    switchText = parseSwitchText(selectedPosts)
                },
                onShowMore = {
                    scope.launch {
                        fetchMyPosts(
                            skip = postToSkip,
                            onSuccess = {
                                if (it is ApiListResponse.Success) {
                                    if (it.data.isNotEmpty()) {
                                        myPosts.addAll(it.data)
                                        postToSkip += POSTS_PER_PAGE
                                        if (it.data.size < POSTS_PER_PAGE) {
                                            shoMoreVisibility = false
                                        }
                                    } else {
                                        shoMoreVisibility = false
                                    }
                                }
                            },
                            onError = { println(it) }
                        )
                    }
                }
            )
        }
    }
}