package br.com.jaquesprojetos.blogmultiplatform.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.com.jaquesprojetos.blogmultiplatform.models.PostWithoutDetails
import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.FONT_FAMILY
import br.com.jaquesprojetos.blogmultiplatform.util.parseDateString
import com.varabyte.kobweb.compose.css.CSSTransition
import com.varabyte.kobweb.compose.css.Cursor
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.ObjectFit
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.css.TextOverflow
import com.varabyte.kobweb.compose.css.TransitionProperty
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
import com.varabyte.kobweb.compose.ui.modifiers.minWidth
import com.varabyte.kobweb.compose.ui.modifiers.objectFit
import com.varabyte.kobweb.compose.ui.modifiers.onClick
import com.varabyte.kobweb.compose.ui.modifiers.overflow
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.size
import com.varabyte.kobweb.compose.ui.modifiers.textOverflow
import com.varabyte.kobweb.compose.ui.modifiers.transition
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.thenIf
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.text.SpanText
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.CSSSizeValue
import org.jetbrains.compose.web.css.CSSUnit
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.ms
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.CheckboxInput


@Composable
fun PostPreview(
    postDetails: PostWithoutDetails,
    selectableMode: Boolean = false,
    darkTheme: Boolean = false,
    vertical: Boolean = true,
    onSelect: (String) -> Unit = {},
    onDeselect: (String) -> Unit = {},
    onClick: (String) -> Unit,
    thumbnailHeight: CSSSizeValue<CSSUnit.px> = 320.px,
    modifier: Modifier = Modifier,
    titleMaxLines: Int = 2,
    titleColor: CSSColorValue = Theme.Black.rgb,

    ) {
    var checked by remember(selectableMode) { mutableStateOf(false) }
    if (vertical) {
        Column(
            modifier = modifier
                .margin(bottom = 24.px, leftRight = 8.px)
                .fillMaxWidth(
                    if (darkTheme) 100.percent
                    else if(titleColor == Theme.Sponsored.rgb) 97.percent
                    else 95.percent
                )
                .padding(all = if (selectableMode) 10.px else 0.px)
                .borderRadius(4.px)
                .border(
                    width = if (selectableMode) 4.px else 0.px,
                    style = if (selectableMode) LineStyle.Solid else LineStyle.None,
                    color = if (checked) Theme.Primary.rgb else Theme.Gray.rgb
                )
                .onClick {
                    if (selectableMode) {
                        checked = !checked
                        if (checked) {
                            onSelect(postDetails._id)
                        } else {
                            onDeselect(postDetails._id)
                        }
                    } else {
                        onClick(postDetails._id)
                    }
                }
                .transition(CSSTransition(property = TransitionProperty.All, duration = 200.ms))
                .cursor(Cursor.Pointer)
        ) {
            PostContent(
                postDetails = postDetails,
                selectableMode = selectableMode,
                vertical = vertical,
                darkTheme = darkTheme,
                onSelect = onSelect,
                onDeselect = onDeselect,
                checked = checked,
                thumbnailHeight = thumbnailHeight,
                titleMaxLines = titleMaxLines,
                titleColor = titleColor
            )
        }

    } else {
        Row(
            modifier = modifier
                .cursor(Cursor.Pointer)
                .onClick { onClick(postDetails._id) }
            ,
        ) {
            PostContent(
                postDetails = postDetails,
                selectableMode = selectableMode,
                vertical = vertical,
                darkTheme = darkTheme,
                onSelect = onSelect,
                onDeselect = onDeselect,
                checked = checked,
                thumbnailHeight = thumbnailHeight,
                titleMaxLines = titleMaxLines,
                titleColor = titleColor
            )
        }
    }
}


@Composable
fun PostContent(
    postDetails: PostWithoutDetails,
    selectableMode: Boolean,
    darkTheme: Boolean,
    onSelect: (String) -> Unit,
    onDeselect: (String) -> Unit,
    vertical: Boolean,
    checked: Boolean,
    thumbnailHeight: CSSSizeValue<CSSUnit.px>,
    titleMaxLines: Int,
    titleColor: CSSColorValue,
) {
    Image(
        modifier = Modifier
            .margin(
                bottom = if (darkTheme) 10.px else 16.px,
                left = if (selectableMode) 10.px else 0.px
            )
            .height(thumbnailHeight)
            .fillMaxWidth(if (vertical) 100.percent else 50.percent)
            .minWidth(316.px) //TODO: Remove this hardcoded magic value and make it a constant
            .objectFit(ObjectFit.Cover)
            .borderRadius(8.px),
        src = postDetails.thumbnail,
        alt = postDetails.title,
    )
    Column(
        modifier = Modifier
            .thenIf(
                condition = !vertical,
                other = Modifier.margin(left = 20.px)
            )
            .padding(all = 5.px)
            .fillMaxWidth()
            .minWidth(316.px), //TODO: Remove this hardcoded magic value and make it a constant


        ) {
        SpanText(
            modifier = Modifier
                .fontFamily(FONT_FAMILY)
                .fontSize(12.px)
                .color(if (darkTheme) Theme.HalfWhite.rgb else Theme.HalfBlack.rgb),
            text = postDetails.date.parseDateString()
        )
        SpanText(
            modifier = Modifier
                .fontFamily(FONT_FAMILY)
                .margin(bottom = 10.px)
                .fontWeight(FontWeight.Bold)
                .color(if (darkTheme) Theme.White.rgb else titleColor)
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
                .fontFamily(FONT_FAMILY)
                .fontSize(16.px)
                .color(if (darkTheme) Theme.White.rgb else Theme.HalfBlack.rgb)
                .textOverflow(TextOverflow.Ellipsis)
                .overflow(Overflow.Hidden)
                .styleModifier {
                    property("display", "-webkit-box")
                    property("-webkit-line-clamp", "$titleMaxLines")
                    property("line-clamp", "$titleMaxLines")
                    property("-webkit-box-orient", "vertical")
                },
            text = postDetails.subtitle
        )
        Row(
            modifier = Modifier
                .fillMaxWidth(if (vertical) 97.percent else 50.percent),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryChip(category = postDetails.category, darkTheme = darkTheme)
            if (selectableMode) {
                CheckboxInput(
                    checked = checked,
                    attrs = Modifier
                        .size(20.px)
                        .toAttrs()
                )
            }
        }
    }
}