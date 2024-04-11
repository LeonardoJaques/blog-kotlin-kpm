package br.com.jaquesprojetos.blogmultiplatform.components

import androidx.compose.runtime.Composable
import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.FONT_FAMILY
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.SIDE_PANEL_WIDTH
import br.com.jaquesprojetos.blogmultiplatform.util.Res
import com.varabyte.kobweb.compose.css.Cursor
import com.varabyte.kobweb.compose.dom.svg.Path
import com.varabyte.kobweb.compose.dom.svg.Svg
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.cursor
import com.varabyte.kobweb.compose.ui.modifiers.fontFamily
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.onClick
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.position
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.modifiers.zIndex
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.text.SpanText
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.vh

@Composable
fun SidePanel() {
    Column(
        modifier = Modifier
            .padding(leftRight = 40.px, topBottom = 50.px)
            .width(SIDE_PANEL_WIDTH.px)
            .height(100.vh)
            .position(Position.Fixed)
            .backgroundColor(Theme.Secondary.rgb)
            .zIndex(9)
    ) {
        Image(
            src = Res.Image.logo,
            description = "Logo image",
            modifier = Modifier.margin(bottom = 16.px)
        )
        SpanText(
            text = "Dashboard",
            modifier = Modifier
                .margin(bottom = 30.px)
                .fontFamily(FONT_FAMILY)
                .fontSize(14.px)
                .color(Theme.HalfWhite.rgb)
        )
        NavigationIcon(
            modifier = Modifier.margin(bottom = 24.px),
            selected = true,
            title = "Home",
            icon = Res.PathIcon.home,
            onClick = {}
        )
        NavigationIcon(
            modifier = Modifier.margin(bottom = 24.px),
            title = "Create Post",
            icon = Res.PathIcon.create,
            onClick = {}
        )
        NavigationIcon(
            modifier = Modifier.margin(bottom = 24.px),
            title = "My Post",
            icon = Res.PathIcon.posts,
            onClick = {}
        )
        NavigationIcon(
            modifier = Modifier.margin(left = 2.px),
            title = "Logout",
            icon = Res.PathIcon.logout,
            onClick = {}
        )

    }
}

@Composable
fun NavigationIcon(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    title: String,
    icon: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .cursor(Cursor.Pointer)
            .onClick { onClick() },
        verticalAlignment = Alignment.CenterVertically

    ) {
        vectorIcon(
            pathData = icon,
            color = if (selected) Theme.Primary.hex else Theme.White.hex,
            modifier = Modifier.margin(right = 10.px)
        )
        SpanText(
            text = title,
            modifier = Modifier
                .fontFamily(FONT_FAMILY)
                .fontSize(16.px)
                .color(if(selected) Theme.Primary.rgb else Theme.White.rgb)
        )
    }
}

@Composable
fun vectorIcon(
    pathData: String,
    color: String,
    modifier: Modifier
) {
    Svg(
        attrs = modifier
            .width(24.px)
            .height(24.px)
            .toAttrs {
                attr("viewBox", "0 0 24 24")
                attr("fill", "none")
            },

        ) {
        Path(
            attrs = modifier.toAttrs {
                attr("d", pathData)
                attr("stroke", color)
                attr("stroke-width", "2")
                attr("stroke-linecap", "round")
                attr("stroke-linejoin", "round")


            }
        )
    }
}