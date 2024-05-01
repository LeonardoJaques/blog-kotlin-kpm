package br.com.jaquesprojetos.blogmultiplatform.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.com.jaquesprojetos.blogmultiplatform.components.SearchBar
import br.com.jaquesprojetos.blogmultiplatform.components.categoryNavigationItems
import br.com.jaquesprojetos.blogmultiplatform.models.Category
import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.navigation.Screen
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.HEADER_HEIGHT
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.PAGE_WIDTH_EX
import br.com.jaquesprojetos.blogmultiplatform.util.Id
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
import com.varabyte.kobweb.compose.ui.modifiers.maxWidth
import com.varabyte.kobweb.compose.ui.modifiers.onClick
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.icons.fa.FaBars
import com.varabyte.kobweb.silk.components.icons.fa.FaXmark
import com.varabyte.kobweb.silk.components.icons.fa.IconSize
import com.varabyte.kobweb.silk.components.style.breakpoint.Breakpoint
import kotlinx.browser.document
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.w3c.dom.HTMLInputElement

@Composable
fun HeaderSection(
    breakpoint: Breakpoint,
    onMenuOpen: () -> Unit,
    logo: String = Res.Image.logoHome,
    selectedCategory: Category? = null,

    ) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .backgroundColor(Theme.Secondary.rgb),


        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .backgroundColor(Theme.Secondary.rgb)
                .fillMaxWidth()
                .maxWidth(PAGE_WIDTH_EX.px),
            contentAlignment = Alignment.TopCenter
        ) {
            Header(
                breakpoint = breakpoint,
                onMenuOpen = onMenuOpen,
                logo = logo,
                selectedCategory = selectedCategory,

                )
        }
    }
}

@Composable
fun Header(
    breakpoint: Breakpoint,
    onMenuOpen: () -> Unit,
    logo: String,

    selectedCategory: Category? = null,
) {
    var fullSearchBarOpened by remember { mutableStateOf(false) }
    val context = rememberPageContext()
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
                    .margin(left = 10.px, right = 50.px)
                    .width(if (breakpoint >= Breakpoint.SM) 100.px else 70.px)
                    .cursor(Cursor.Pointer)
                    .onClick {
                        context.router.navigateTo(Screen.HomePage.route)
                    },
                src = logo,
                description = "Logo Image"
            )
        }

        if (breakpoint >= Breakpoint.LG) {
            categoryNavigationItems(
                onMenuOpen = onMenuOpen,
                selectedCategory = selectedCategory,
            )
        }
        Spacer()
        SearchBar(
            fullWidth = fullSearchBarOpened,
            breakpoint = breakpoint,
            darkTheme = true,
            onEnterClick = {
                val query =
                    (document.getElementById(
                        Id.adminSearchBar
                    ) as HTMLInputElement).value
                context.router.navigateTo(Screen.SearchPage.searchByTitle(query = query))
            },
            onSearchIconClick = {
                fullSearchBarOpened = it
            }
        )
    }
}


