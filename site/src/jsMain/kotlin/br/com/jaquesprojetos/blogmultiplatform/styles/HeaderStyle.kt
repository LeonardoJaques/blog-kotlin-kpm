package br.com.jaquesprojetos.blogmultiplatform.styles

import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import com.varabyte.kobweb.compose.css.Transition
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.transition
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.selectors.anyLink
import com.varabyte.kobweb.silk.style.selectors.hover
import org.jetbrains.compose.web.css.ms

val CategoryItemStyle = CssStyle {
    base {
        Modifier
            .color(Theme.White.rgb)
            .transition(Transition.of(
                property = "color",
                duration = 200.ms
            ))
    }
    anyLink{
        Modifier
            .color(Theme.White.rgb)
    }
    hover{
        Modifier
            .color(Theme.Primary.rgb)

    }
}
