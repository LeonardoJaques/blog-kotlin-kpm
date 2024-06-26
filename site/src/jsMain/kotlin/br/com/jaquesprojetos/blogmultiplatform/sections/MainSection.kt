package br.com.jaquesprojetos.blogmultiplatform.sections

import androidx.compose.runtime.Composable
import br.com.jaquesprojetos.blogmultiplatform.components.LoadingIndicator
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
    onClick: (String) -> Unit,
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
                    LoadingIndicator()
                }

                is ApiListResponse.Success -> {
                    MainPosts(
                        breakpoint = breakpoint,
                        posts = posts.data,
                        onClick = onClick
                    )
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
    onClick: (String) -> Unit,
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
                    thumbnailHeight = 595.px,
                    onClick = { onClick(posts.first()._id) }

                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth(55.percent)
                        .margin(left = 20.px),
                ) {
                    posts.drop(1).forEach { postDetails ->
                        PostPreview(
                            postDetails = postDetails,
                            darkTheme = true,
                            vertical = false,
                            thumbnailHeight = 175.px,
                            modifier = Modifier
                                .margin(bottom = 25.px)
                                .fillMaxWidth(),
                            titleMaxLines = 3,
                            onClick = { onClick(postDetails._id) }
                        )
                    }
                }
            }

            breakpoint >= Breakpoint.LG -> {
                posts.take(2).forEach { postDetails ->
                    PostPreview(
                        postDetails = postDetails,
                        darkTheme = true,
                        vertical = true,
                        onClick = { onClick(postDetails._id)}
                    )
                }
            }

            breakpoint <= Breakpoint.MD -> {
                posts.take(1).forEach { postDetails ->
                    PostPreview(
                        postDetails = postDetails,
                        darkTheme = true,
                        vertical = true,
                        onClick = { onClick(postDetails._id)}

                    )
                }
            }

            else -> {
                posts.forEach { postDetails ->
                    PostPreview(
                        postDetails = postDetails,
                        darkTheme = true,
                        vertical = true,
                        onClick = { onClick(postDetails._id)}
                    )
                }
            }
        }
    }
}