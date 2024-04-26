package br.com.jaquesprojetos.blogmultiplatform.sections

import androidx.compose.runtime.Composable
import br.com.jaquesprojetos.blogmultiplatform.components.PostPreview
import br.com.jaquesprojetos.blogmultiplatform.models.ApiListResponse
import br.com.jaquesprojetos.blogmultiplatform.models.PostWithoutDetails
import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.PAGE_WIDTH_EX
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.maxWidth
import com.varabyte.kobweb.silk.components.style.breakpoint.Breakpoint
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px

@Composable
fun MainSection(
    posts: ApiListResponse,
    breakpoint: Breakpoint,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .backgroundColor(Theme.Secondary.rgb),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .maxWidth(PAGE_WIDTH_EX.px)
                .backgroundColor(Theme.Secondary.rgb),
            contentAlignment = Alignment.Center,
        ) {
            when (posts) {
                is ApiListResponse.Idle -> {
                    // Loading
                }

                is ApiListResponse.Success -> {
                    MainPosts(breakpoint = breakpoint, posts = posts.data)
                }

                is ApiListResponse.Error -> {
                    // Error
                }
            }
        }
    }
}

@Composable
fun MainPosts(
    posts: List<PostWithoutDetails>,
    breakpoint: Breakpoint,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(
                if (breakpoint > Breakpoint.MD) 80.percent else 90.percent
            )
            .margin(topBottom = 20.px),
    ) {
        when {
            breakpoint == Breakpoint.XL -> {
                PostPreview(
                    postDetails = posts.first(),
                    darkTheme = true,
                    vertical = true,
                    thumbnailHeight = 600.px,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth(50.percent)
                ) {
                    posts.drop(1).forEach { post ->
                        PostPreview(
                            postDetails = post,
                            darkTheme = true,
                            vertical = false,
                            thumbnailHeight = 180.px,
                            modifier = Modifier
                                .margin(bottom = 25.px)
                                .fillMaxWidth(),
                            textMaxLines = 1
                        )
                    }
                }
            }

            breakpoint >= Breakpoint.LG -> {
                posts.take(2).forEach { post ->
                    PostPreview(
                        postDetails = post,
                        darkTheme = true,
                        vertical = true,
                    )
                }
            }

            breakpoint <= Breakpoint.MD -> {
                posts.take(1).forEach { post ->
                    PostPreview(
                        postDetails = post,
                        darkTheme = true,
                        vertical = true,

                        )
                }
            }

            else -> {
                posts.forEach { post ->
                    PostPreview(
                        postDetails = post,
                        darkTheme = true,
                        vertical = true,

                        )
                }
            }
        }
    }
}