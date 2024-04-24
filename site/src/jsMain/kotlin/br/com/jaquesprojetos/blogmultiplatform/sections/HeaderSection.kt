package br.com.jaquesprojetos.blogmultiplatform.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.com.jaquesprojetos.blogmultiplatform.components.SearchBar
import br.com.jaquesprojetos.blogmultiplatform.components.categoryNavigationItems

import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.HEADER_HEIGHT
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.PAGE_WIDTH
import br.com.jaquesprojetos.blogmultiplatform.util.Res
import com.varabyte.kobweb.compose.css.Cursor
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.foundation.layout.Spacer
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.cursor
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.onClick
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.icons.fa.FaBars
import com.varabyte.kobweb.silk.components.icons.fa.FaXmark
import com.varabyte.kobweb.silk.components.icons.fa.IconSize
import com.varabyte.kobweb.silk.components.style.breakpoint.Breakpoint
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.vw

@Composable
fun HeaderSection(
    breakpoint: Breakpoint,
    onMenuOpen: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .width(PAGE_WIDTH.vw)
            .backgroundColor(Theme.Secondary.rgb),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .backgroundColor(Theme.Secondary.rgb)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Header(
                breakpoint = breakpoint,
                onMenuOpen = onMenuOpen
            )
        }
    }
}

@Composable
fun Header(
    breakpoint: Breakpoint,
    onMenuOpen: () -> Unit,
) {
    var fullSearchBarOpened by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth(if (breakpoint > Breakpoint.MD) 80.percent else 90.percent)
            .height(HEADER_HEIGHT.px),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (fullSearchBarOpened) {
            if (breakpoint <= Breakpoint.MD) {
                FaXmark(
                    size = IconSize.XL,
                    modifier = Modifier
                        .margin(right = 24.px)
                        .color(Theme.White.rgb)
                        .cursor(Cursor.Pointer)
                        .onClick { fullSearchBarOpened = false }
                )
            }
        }
        if (!fullSearchBarOpened) {
            if (breakpoint <= Breakpoint.MD) {
                FaBars(
                    size = IconSize.XL,
                    modifier = Modifier
                        .margin(right = 24.px)
                        .color(Theme.White.rgb)
                        .cursor(Cursor.Pointer)
                        .onClick { onMenuOpen() }
                )
            }

        }

        if (!fullSearchBarOpened) {
            Image(
                modifier = Modifier
                    .margin(right = 50.px)
                    .width(if (breakpoint >= Breakpoint.SM) 100.px else 70.px)
                    .cursor(Cursor.Pointer)
                    .onClick { },
                src = Res.Image.logoHome,
                description = "Logo Image"
            )
        }

        if (breakpoint >= Breakpoint.LG) {
            categoryNavigationItems(onMenuOpen)
        }
        Spacer()
        SearchBar(
            fullWidth = fullSearchBarOpened,
            breakpoint = breakpoint,
            darkTheme = true,
            onEnterClick = {},
            onSearchIconClick = {
                fullSearchBarOpened = it
            }
        )
    }
}


