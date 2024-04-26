package br.com.jaquesprojetos.blogmultiplatform.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.com.jaquesprojetos.blogmultiplatform.components.OverflowSidePanel
import br.com.jaquesprojetos.blogmultiplatform.components.categoryNavigationItems
import br.com.jaquesprojetos.blogmultiplatform.models.ApiListResponse
import br.com.jaquesprojetos.blogmultiplatform.sections.HeaderSection
import br.com.jaquesprojetos.blogmultiplatform.sections.MainSection
import br.com.jaquesprojetos.blogmultiplatform.util.fetchMainPosts
import com.varabyte.kobweb.compose.foundation.layout.Arrangement
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.theme.breakpoint.rememberBreakpoint

@Page
@Composable
fun HomePage() {
    val breakpoint = rememberBreakpoint()
    var overflowMenuOpened by remember { mutableStateOf(false) }
    var mainPosts by remember { mutableStateOf<ApiListResponse>(ApiListResponse.Idle) }
    LaunchedEffect(Unit) {
        fetchMainPosts(
            onSuccess = { mainPosts = it
                        println(mainPosts)
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
            breakpoint = breakpoint
        )
    }
}
