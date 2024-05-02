package br.com.jaquesprojetos.blogmultiplatform.pages.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import br.com.jaquesprojetos.blogmultiplatform.components.LoadingIndicator
import br.com.jaquesprojetos.blogmultiplatform.components.OverflowSidePanel
import br.com.jaquesprojetos.blogmultiplatform.components.categoryNavigationItems
import br.com.jaquesprojetos.blogmultiplatform.models.ApiListResponse
import br.com.jaquesprojetos.blogmultiplatform.models.Category
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.CATEGORY_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.POSTS_PER_PAGE
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.QUERY_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.PostWithoutDetails
import br.com.jaquesprojetos.blogmultiplatform.navigation.Screen
import br.com.jaquesprojetos.blogmultiplatform.sections.FooterSection
import br.com.jaquesprojetos.blogmultiplatform.sections.HeaderSection
import br.com.jaquesprojetos.blogmultiplatform.sections.PostsSection
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.FONT_FAMILY
import br.com.jaquesprojetos.blogmultiplatform.util.Id
import br.com.jaquesprojetos.blogmultiplatform.util.Res
import br.com.jaquesprojetos.blogmultiplatform.util.searchPostByTitle
import br.com.jaquesprojetos.blogmultiplatform.util.searchPostsByCategory
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.TextAlign
import com.varabyte.kobweb.compose.foundation.layout.Arrangement
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.fontFamily
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.textAlign
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.theme.breakpoint.rememberBreakpoint
import kotlinx.browser.document
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.px
import org.w3c.dom.HTMLInputElement

@Page(routeOverride = "query")
@Composable
fun SearchPage() {
    val breakpoint = rememberBreakpoint()
    val context = rememberPageContext()
    val scope = rememberCoroutineScope()

    var overflowMenuOpened by remember { mutableStateOf(false) }
    val searchPost = remember { mutableStateListOf<PostWithoutDetails>() }
    var postsToSkip by remember { mutableStateOf(0) }
    var showMorePosts by remember { mutableStateOf(false) }

    var apiResponse by remember { mutableStateOf<ApiListResponse>(ApiListResponse.Idle) }

    val hasCategoryParam = remember(key1 = context.route) {
        context.route.params.containsKey(CATEGORY_PARAM)
    }
    val hasQueryParam = remember(key1 = context.route) {
        context.route.params.containsKey(QUERY_PARAM)
    }
    val value = remember(key1 = context.route) {
        if (hasCategoryParam) {
            context.route.params.getValue(CATEGORY_PARAM)
        } else if (hasQueryParam) {
            context.route.params.getValue(QUERY_PARAM)
        } else {
            ""
        }
    }
    LaunchedEffect(key1 = context.route) {
        (document.getElementById(Id.adminSearchBar) as HTMLInputElement).value = ""
        showMorePosts = false
        postsToSkip = 0

        if (hasCategoryParam) {
            searchPostsByCategory(
                category = runCatching { Category.valueOf(value) }.getOrDefault(Category.Technology),
                skip = postsToSkip,
                onSuccess = { response ->
                    apiResponse = response
                    if (response is ApiListResponse.Success) {
                        searchPost.clear()
                        searchPost.addAll(response.data)
                        postsToSkip += POSTS_PER_PAGE
                        if (response.data.size >= POSTS_PER_PAGE) showMorePosts = true
                    }
                },
                onError = {}
            )
        } else if (hasQueryParam) {
            (document.getElementById(Id.adminSearchBar) as HTMLInputElement).value = value
            searchPostByTitle(
                query = value,
                skip = postsToSkip,
                onSuccess = { response ->
                    apiResponse = response
                    if (response is ApiListResponse.Success) {
                        searchPost.clear()
                        searchPost.addAll(response.data)
                        postsToSkip += POSTS_PER_PAGE
                        if (response.data.size >= POSTS_PER_PAGE) showMorePosts = true
                    }
                },
                onError = {}
            )
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (overflowMenuOpened) {
            OverflowSidePanel(
                onMenuClosed = { overflowMenuOpened = false },
                content = {
                    categoryNavigationItems(
                        onMenuOpen = { overflowMenuOpened = true },
                        vertical = true,
                        selectedCategory = if (hasCategoryParam) runCatching {
                            Category.valueOf(
                                value
                            )
                        }.getOrDefault(Category.Technology) else null,
                    )
                }
            )
        }
        HeaderSection(
            logo = Res.Image.logo,
            breakpoint = breakpoint,
            onMenuOpen = { overflowMenuOpened = true },
            selectedCategory = if (hasCategoryParam) runCatching {
                Category.valueOf(
                    value
                )
            }.getOrDefault(Category.Technology) else null,
        )
        if (apiResponse is ApiListResponse.Success) {
            if (hasCategoryParam) {
                SpanText(
                    modifier = Modifier
                        .fontFamily(FONT_FAMILY)
                        .fontWeight(FontWeight.Medium)
                        .margin(top = 40.px, bottom = 100.px)
                        .fillMaxWidth()
                        .fontSize(30.px)
                        .textAlign(TextAlign.Center),
                    text = value.ifEmpty {
                        Category.Technology.name
                    }
                )
            }
            PostsSection(
                breakpoint = breakpoint,
                posts = searchPost,
                showMoreVisibility = showMorePosts,
                onShowMore = {
                    scope.launch {
                        if (hasCategoryParam) {
                            searchPostsByCategory(
                                skip = postsToSkip,
                                category = Category.valueOf(value),
                                onSuccess = { response ->
                                    if (response is ApiListResponse.Success) {
                                        if (response.data.isNotEmpty()) {
                                            if (response.data.size <= POSTS_PER_PAGE) {
                                                showMorePosts = false
                                                searchPost.addAll(response.data)
                                                postsToSkip += POSTS_PER_PAGE
                                            } else {
                                                showMorePosts = false
                                            }
                                        }
                                    }
                                },
                                onError = {}
                            )
                        } else if (hasQueryParam) {
                            searchPostByTitle(
                                skip = postsToSkip,
                                query = value,
                                onSuccess = { response ->
                                    if (response is ApiListResponse.Success) {
                                        if (response.data.isNotEmpty()) {
                                            if (response.data.size <= POSTS_PER_PAGE) {
                                                showMorePosts = false
                                                searchPost.addAll(response.data)
                                                postsToSkip += POSTS_PER_PAGE
                                            } else {
                                                showMorePosts = false
                                            }
                                        }
                                    }
                                },
                                onError = {}
                            )
                        }
                    }
                },
                onClick = {
                    context.router.navigateTo(Screen.PostPage.getPost(id = it))
                }
            )
        } else{
            LoadingIndicator()
        }
        FooterSection()
    }
}