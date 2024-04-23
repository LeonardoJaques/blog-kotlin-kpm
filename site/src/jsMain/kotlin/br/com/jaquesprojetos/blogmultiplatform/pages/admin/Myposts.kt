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
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.POSTS_PER_PAGE
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.QUERY_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.PostWithoutDetails
import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.navigation.Screen
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.FONT_FAMILY
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.SIDE_PANEL_WIDTH
import br.com.jaquesprojetos.blogmultiplatform.util.Id
import br.com.jaquesprojetos.blogmultiplatform.util.deleteSelectedPosts
import br.com.jaquesprojetos.blogmultiplatform.util.fetchMyPosts
import br.com.jaquesprojetos.blogmultiplatform.util.isUserLoggedIn
import br.com.jaquesprojetos.blogmultiplatform.util.noBorder
import br.com.jaquesprojetos.blogmultiplatform.util.parseSwitchText
import br.com.jaquesprojetos.blogmultiplatform.util.searchPostByTitle
import com.varabyte.kobweb.compose.css.CSSTransition
import com.varabyte.kobweb.compose.css.TransitionProperty
import com.varabyte.kobweb.compose.css.Visibility
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
import com.varabyte.kobweb.compose.ui.modifiers.transition
import com.varabyte.kobweb.compose.ui.modifiers.visibility
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.silk.components.forms.Switch
import com.varabyte.kobweb.silk.components.forms.SwitchSize
import com.varabyte.kobweb.silk.components.style.breakpoint.Breakpoint
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.theme.breakpoint.rememberBreakpoint
import kotlinx.browser.document
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.ms
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.w3c.dom.HTMLInputElement

@Page
@Composable
fun MyPostsPage() {
    isUserLoggedIn { MyPostsScreen() }
}

@Composable
fun MyPostsScreen() {
    val breakpoint = rememberBreakpoint()
    val scope = rememberCoroutineScope()
    val context = rememberPageContext()

    val myPosts = remember { mutableStateListOf<PostWithoutDetails>() }
    val selectedPosts = remember { mutableStateListOf<String>() }

    var selectableMode by remember { mutableStateOf(false) }
    var shoMoreVisibility by remember { mutableStateOf(false) }
    var switchText by remember { mutableStateOf("Select") }
    var postToSkip by remember { mutableStateOf(0) }

    val hasParams = remember(key1 = context.route) { context.route.params.containsKey(QUERY_PARAM) }
    val query = remember(key1 = context.route) { context.route.params[QUERY_PARAM] ?: "" }
    LaunchedEffect(context.route) {
        postToSkip = 0
        if (hasParams) {
            document.getElementById(Id.adminSearchBar)?.let {
                (it as HTMLInputElement).value = query.replace("%20", " ")
            }
            searchPostByTitle(
                query = query,
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
        } else {
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
                    modifier = Modifier
                        .visibility(
                            if (selectableMode) Visibility.Hidden
                            else Visibility.Visible

                        )
                        .transition(
                            CSSTransition(
                                property = TransitionProperty.All,
                                duration = 200.ms
                            )
                        ),
                    onEnterClick = {
                        val queryParam =
                            (document.getElementById(Id.adminSearchBar) as HTMLInputElement).value
                        if (queryParam.isNotEmpty()) {
                            context.router.navigateTo(Screen.AdminMyPosts.searchByTitle(queryParam))
                        } else {
                            context.router.navigateTo(Screen.AdminMyPosts.route)
                        }
                    }
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
                        checked = selectableMode,
                        onCheckedChange = {
                            selectableMode = it
                            if (!selectableMode) {
                                switchText = "Select"
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
                                if (selectableMode) Theme.Black.rgb
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
                        .visibility(if (selectedPosts.isNotEmpty()) Visibility.Visible else Visibility.Hidden)
                        .onClick {
                            scope.launch {
                                val result = deleteSelectedPosts(ids = selectedPosts)
                                if (result) {
                                    selectableMode = false
                                    switchText = "Select"
                                    postToSkip -= selectedPosts.size
                                    selectedPosts.forEach { deletedPostId ->
                                        myPosts.removeAll {
                                            it._id == deletedPostId
                                        }
                                    }
                                    selectedPosts.clear()
                                }
                            }
                        }
                        .toAttrs()
                ) {
                    SpanText(text = "Delete")
                }
            }
            Posts(
                post = myPosts,
                breakpoint = breakpoint,
                showMoreVisibility = shoMoreVisibility,
                selectableMode = selectableMode,
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
                        if (hasParams) {
                            searchPostByTitle(
                                query = query,
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
                        } else {
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
                }
            )
        }
    }
}