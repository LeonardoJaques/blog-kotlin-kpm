package br.com.jaquesprojetos.blogmultiplatform.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import br.com.jaquesprojetos.blogmultiplatform.components.OverflowSidePanel
import br.com.jaquesprojetos.blogmultiplatform.components.categoryNavigationItems
import br.com.jaquesprojetos.blogmultiplatform.models.ApiListResponse
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.POSTS_PER_PAGE
import br.com.jaquesprojetos.blogmultiplatform.models.PostWithoutDetails
import br.com.jaquesprojetos.blogmultiplatform.sections.HeaderSection
import br.com.jaquesprojetos.blogmultiplatform.sections.MainSection
import br.com.jaquesprojetos.blogmultiplatform.sections.NewsletterSection
import br.com.jaquesprojetos.blogmultiplatform.sections.PostsSection
import br.com.jaquesprojetos.blogmultiplatform.sections.SponsoredSection
import br.com.jaquesprojetos.blogmultiplatform.util.fetchLatestPosts
import br.com.jaquesprojetos.blogmultiplatform.util.fetchMainPosts
import br.com.jaquesprojetos.blogmultiplatform.util.fetchPopularPosts
import br.com.jaquesprojetos.blogmultiplatform.util.fetchSponsoredPosts
import com.varabyte.kobweb.compose.foundation.layout.Arrangement
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.theme.breakpoint.rememberBreakpoint
import kotlinx.coroutines.launch

@Page
@Composable
fun HomePage() {
    val scope = rememberCoroutineScope()
    val breakpoint = rememberBreakpoint()
    var overflowMenuOpened by remember { mutableStateOf(false) }
    var mainPosts by remember { mutableStateOf<ApiListResponse>(ApiListResponse.Idle) }
    val latestPosts = remember { mutableStateListOf<PostWithoutDetails>() }
    val sponsoredPosts = remember { mutableStateListOf<PostWithoutDetails>() }
    val popularPosts = remember { mutableStateListOf<PostWithoutDetails>() }
    var popularPostsToSkip by remember { mutableStateOf(0) }
    var latestPostsToSkip by remember { mutableStateOf(0) }
    var showMoreLatestPosts by remember { mutableStateOf(false) }
    var showMorePopularPosts by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        fetchMainPosts(
            onSuccess = { mainPosts = it },
            onError = {}
        )
        fetchLatestPosts(
            skip = latestPostsToSkip,
            onSuccess = { response ->
                if (response is ApiListResponse.Success) {
                    latestPosts.addAll(response.data)
                    latestPostsToSkip += POSTS_PER_PAGE
                    if (response.data.size >= POSTS_PER_PAGE) showMoreLatestPosts = true
                }
            },
            onError = {}
        )
        fetchPopularPosts(
            skip = popularPostsToSkip,
            onSuccess = { response ->
                if (response is ApiListResponse.Success) {
                    popularPosts.addAll(response.data)
                    popularPostsToSkip += POSTS_PER_PAGE
                    if (response.data.size >= POSTS_PER_PAGE) showMorePopularPosts = true
                }
            },
            onError = {}
        )

        fetchSponsoredPosts(
            onSuccess = { response ->
                if (response is ApiListResponse.Success) {
                    sponsoredPosts.addAll(response.data)
                }
            },
            onError = {}
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
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
            onMenuOpen = { overflowMenuOpened = true }
        )
        MainSection(
            posts = mainPosts,
            breakpoint = breakpoint,
            onClick = {}

        )

        PostsSection(
            breakpoint = breakpoint,
            posts = latestPosts,
            title = "Latest Posts",
            showMoreVisibility = showMoreLatestPosts,
            onShowMore = {
                scope.launch {
                    fetchLatestPosts(
                        skip = latestPostsToSkip,
                        onSuccess = { response ->
                            if (response is ApiListResponse.Success) {
                                if (response.data.isNotEmpty()) {
                                    if (response.data.size < POSTS_PER_PAGE) {
                                        showMoreLatestPosts = false
                                        latestPosts.addAll(response.data)
                                        latestPostsToSkip += POSTS_PER_PAGE
                                    } else {
                                        showMoreLatestPosts = false
                                    }
                                }
                            }
                        },
                        onError = {}
                    )
                }
            },
            onClick = {}
        )

        SponsoredSection(
            breakpoint = breakpoint,
            posts = sponsoredPosts,
            onClick = {}
        )

        PostsSection(
            breakpoint = breakpoint,
            posts = popularPosts,
            title = "Popular Posts",
            showMoreVisibility = showMorePopularPosts,
            onShowMore = {
                scope.launch {
                    fetchPopularPosts(
                        skip = popularPostsToSkip,
                        onSuccess = { response ->
                            if (response is ApiListResponse.Success) {
                                if (response.data.isNotEmpty()) {
                                    if (response.data.size < POSTS_PER_PAGE) {
                                        showMorePopularPosts = false
                                        popularPosts.addAll(response.data)
                                        popularPostsToSkip += POSTS_PER_PAGE
                                    } else {
                                        showMorePopularPosts = false
                                    }
                                }
                            }
                        },
                        onError = {}
                    )
                }
            },
            onClick = {}
        )
        NewsletterSection(
            breakpoint = breakpoint
        )
    }
}
