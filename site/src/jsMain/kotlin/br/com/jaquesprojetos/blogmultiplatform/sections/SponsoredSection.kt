package br.com.jaquesprojetos.blogmultiplatform.sections

import androidx.compose.runtime.Composable
import br.com.jaquesprojetos.blogmultiplatform.components.PostPreview
import br.com.jaquesprojetos.blogmultiplatform.models.PostWithoutDetails
import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.FONT_FAMILY
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.PAGE_WIDTH_EX
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Arrangement
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.fontFamily
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.maxWidth
import com.varabyte.kobweb.silk.components.icons.fa.FaTag
import com.varabyte.kobweb.silk.components.icons.fa.IconSize
import com.varabyte.kobweb.silk.components.layout.SimpleGrid
import com.varabyte.kobweb.silk.components.layout.numColumns
import com.varabyte.kobweb.silk.components.style.breakpoint.Breakpoint
import com.varabyte.kobweb.silk.components.text.SpanText
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px

@Composable
fun SponsoredSection(
    breakpoint: Breakpoint,
    posts: List<PostWithoutDetails>,
    onClick: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .margin(bottom = 100.px)
            .backgroundColor(Theme.LightGray.rgb),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .maxWidth(PAGE_WIDTH_EX.px)
                .margin(topBottom = 50.px),
            contentAlignment = Alignment.TopCenter
        ) {
            SponsoredPosts(
                breakpoint = breakpoint,
                posts = posts,
                onClick = onClick
            )
        }
    }
}

@Composable
fun SponsoredPosts(
    breakpoint: Breakpoint,
    posts: List<PostWithoutDetails>,
    onClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(
            if (breakpoint > Breakpoint.MD) 80.percent
            else 90.percent
        ),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.margin(bottom = 30.px),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FaTag(
                modifier = Modifier
                    .margin(right = 10.px)
                    .color(Theme.Sponsored.rgb),
                size = IconSize.XL
            )
            SpanText(
                modifier = Modifier
                    .fontFamily(FONT_FAMILY)
                    .fontSize(18.px)
                    .fontWeight(FontWeight.Medium)
                    .color(Theme.Sponsored.rgb),
                text = "Sponsored Posts"
            )
        }
        SimpleGrid(
            modifier = Modifier
                .fillMaxWidth(),
            numColumns = numColumns(base = 1, sm = 1, md = 2, lg = 3, xl = 4)
        ) {
            posts.forEach { post ->
                PostPreview(
                    modifier = Modifier.margin(right = 50.px),
                    postDetails = post,
                    vertical = true,
                    titleMaxLines = 1,
                    thumbnailHeight = if (breakpoint >= Breakpoint.MD) 200.px else 300.px,
                    onClick = onClick,
                    titleColor = Theme.Sponsored.rgb
                )
            }
        }
    }
}