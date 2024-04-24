package br.com.jaquesprojetos.blogmultiplatform.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.com.jaquesprojetos.blogmultiplatform.models.PostWithoutDetails
import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.navigation.Screen
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.FONT_FAMILY
import br.com.jaquesprojetos.blogmultiplatform.util.parseDateString
import com.varabyte.kobweb.compose.css.CSSTransition
import com.varabyte.kobweb.compose.css.Cursor
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.ObjectFit
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.css.TextAlign
import com.varabyte.kobweb.compose.css.TextOverflow
import com.varabyte.kobweb.compose.css.TransitionProperty
import com.varabyte.kobweb.compose.css.Visibility
import com.varabyte.kobweb.compose.foundation.layout.Arrangement
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.border
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.cursor
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.fontFamily
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.objectFit
import com.varabyte.kobweb.compose.ui.modifiers.onClick
import com.varabyte.kobweb.compose.ui.modifiers.overflow
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.size
import com.varabyte.kobweb.compose.ui.modifiers.textAlign
import com.varabyte.kobweb.compose.ui.modifiers.textOverflow
import com.varabyte.kobweb.compose.ui.modifiers.transition
import com.varabyte.kobweb.compose.ui.modifiers.visibility
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.silk.components.forms.Checkbox
import com.varabyte.kobweb.silk.components.forms.CheckboxSize
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.layout.SimpleGrid
import com.varabyte.kobweb.silk.components.layout.numColumns
import com.varabyte.kobweb.silk.components.style.breakpoint.Breakpoint
import com.varabyte.kobweb.silk.components.text.SpanText
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.ms
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px


@Composable
fun PostPreview(
    postDetails: PostWithoutDetails,
    selectableMode: Boolean,
    onSelect: (String) -> Unit,
    onDeselect: (String) -> Unit,
) {
    var checkced by remember(selectableMode) { mutableStateOf(false) }
    val context = rememberPageContext()

    Column(
        modifier = Modifier
            .padding(all = 12.px)
            .margin(bottom = 24.px, leftRight = 8.px)
            .fillMaxWidth(90.percent)
            .padding(all = if (selectableMode) 10.px else 0.px)
            .borderRadius(4.px)
            .border(
                width = if (selectableMode) 4.px else 0.px,
                style = if (selectableMode) LineStyle.Solid else LineStyle.None,
                color = if (checkced) Theme.Primary.rgb else Theme.Gray.rgb
            )
            .onClick {
                if (selectableMode) {
                    checkced = !checkced
                    if (checkced) {
                        onSelect(postDetails._id)
                    } else {
                        onDeselect(postDetails._id)
                    }
                } else {
                    context.router.navigateTo(Screen.AdminCreate.passPostId(postDetails._id))
                }
            }
            .transition(CSSTransition(property = TransitionProperty.All, duration = 200.ms))
            .cursor(Cursor.Pointer)
    ) {
        Image(
            modifier = Modifier
                .margin(bottom = 5.px)
                .height(320.px)
                .width(370.px)
                .objectFit(ObjectFit.Cover)
                .borderRadius(8.px),
            src = postDetails.thumbnail,
            alt = postDetails.title,
        )

        Column(
            modifier = Modifier
                .size(370.px, 160.px),

            verticalArrangement = Arrangement.SpaceAround

        ) {
            SpanText(
                modifier = Modifier
                    .fontFamily(FONT_FAMILY)
                    .fontSize(12.px)
                    .color(Theme.HalfBlack.rgb),
                text = postDetails.date.parseDateString()
            )
            SpanText(
                modifier = Modifier
                    .fontFamily(FONT_FAMILY)
                    .margin(bottom = 10.px)
                    .width(370.px)
                    .fontWeight(FontWeight.Bold)
                    .color(Theme.Black.rgb)
                    .textOverflow(TextOverflow.Ellipsis)
                    .overflow(Overflow.Hidden)
                    .styleModifier {
                        property("display", "-webkit-box")
                        property("-webkit-line-clamp", "1")
                        property("line-clamp", "1")
                        property("-webkit-box-orient", "vertical")
                    },
                text = postDetails.title
            )
            SpanText(
                modifier = Modifier
                    .margin(bottom = 8.px)
                    .width(370.px)
                    .fontFamily(FONT_FAMILY)
                    .fontSize(16.px)
                    .color(Theme.Black.rgb)
                    .textOverflow(TextOverflow.Ellipsis)
                    .overflow(Overflow.Hidden)
                    .styleModifier {
                        property("display", "-webkit-box")
                        property("-webkit-line-clamp", "2")
                        property("line-clamp", "2")
                        property("-webkit-box-orient", "vertical")
                    },
                text = postDetails.subtitle
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                ) {
                CategoryChip(category = postDetails.category)
                if (selectableMode) {
                    Checkbox(
                        size = CheckboxSize.LG,
                        checked = checkced,
                        onCheckedChange = { checkced = it },
                        modifier = Modifier
                            .color(Theme.Primary.rgb)
                    )
                }
            }
        }
    }

}

@Composable
fun Posts(
    post: List<PostWithoutDetails>,
    breakpoint: Breakpoint,
    showMoreVisibility: Boolean,
    onShowMore: () -> Unit,
    selectableMode: Boolean = false,
    onSelect: (String) -> Unit,
    onDeselect: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(if (breakpoint > Breakpoint.MD) 80.percent else 90.percent),
        verticalArrangement = Arrangement.Center
    ) {
        SimpleGrid(
            modifier = Modifier.fillMaxWidth(),
            numColumns = numColumns(base = 1, sm = 2, md = 3, lg = 4)
        ) {
            post.forEach {
                PostPreview(
                    postDetails = it,
                    selectableMode = selectableMode,
                    onSelect = onSelect,
                    onDeselect = onDeselect
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
                .onClick { onShowMore() }
                .visibility(if (showMoreVisibility) Visibility.Visible else Visibility.Hidden)
                .color(Theme.HalfBlack.rgb),

            text = "Show More"
        )
    }
}