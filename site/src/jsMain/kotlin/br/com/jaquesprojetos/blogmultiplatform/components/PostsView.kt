package br.com.jaquesprojetos.blogmultiplatform.components

import androidx.compose.runtime.Composable
import br.com.jaquesprojetos.blogmultiplatform.models.PostWithoutDetails
import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.FONT_FAMILY
import com.varabyte.kobweb.compose.css.Cursor
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.TextAlign
import com.varabyte.kobweb.compose.css.Visibility
import com.varabyte.kobweb.compose.foundation.layout.Arrangement
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.cursor
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.fontFamily
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.onClick
import com.varabyte.kobweb.compose.ui.modifiers.textAlign
import com.varabyte.kobweb.compose.ui.modifiers.visibility
import com.varabyte.kobweb.silk.components.layout.SimpleGrid
import com.varabyte.kobweb.silk.components.layout.numColumns
import com.varabyte.kobweb.silk.components.style.breakpoint.Breakpoint
import com.varabyte.kobweb.silk.components.text.SpanText
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px

@Composable
fun PostsView(
    breakpoint: Breakpoint,
    title: String? = null,
    post: List<PostWithoutDetails>,
    showMoreVisibility: Boolean,
    onShowMore: () -> Unit,
    selectableMode: Boolean = false,
    onSelect: (String) -> Unit = {},
    onDeselect: (String) -> Unit = {},
    onClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(if (breakpoint > Breakpoint.MD) 80.percent else 90.percent),
        verticalArrangement = Arrangement.Center
    ) {
        if (title != null) {
            SpanText(
                modifier = Modifier
                    .fillMaxWidth()
                    .margin(topBottom = 24.px, leftRight = 14.px)
                    .fontFamily(FONT_FAMILY)
                    .fontSize(18.px)
                    .fontWeight(FontWeight.Medium),
                text = title
            )
        }
        SimpleGrid(
            modifier = Modifier.fillMaxWidth(),
            numColumns = numColumns(base = 1, sm = 2, md = 3, lg = 3)
        ) {
            post.forEach {
                PostPreview(
                    postDetails = it,
                    selectableMode = selectableMode,
                    onSelect = onSelect,
                    onDeselect = onDeselect,
                    onClick = onClick
                )
            }
        }
        SpanText(
            modifier = Modifier
                .fillMaxWidth()
                .textAlign(TextAlign.Center)
                .margin(topBottom = 50.px)
                .fontFamily(FONT_FAMILY)
                .fontSize(16.px)
                .fontWeight(FontWeight.Medium)
                .cursor(Cursor.Pointer)
                .onClick {
                    onShowMore()
                }
                .visibility(if (showMoreVisibility) Visibility.Visible else Visibility.Hidden)
                .color(Theme.Black.rgb),
            text = "Show More" //TODO: Remove this magic word and make it a constant
        )
    }
}