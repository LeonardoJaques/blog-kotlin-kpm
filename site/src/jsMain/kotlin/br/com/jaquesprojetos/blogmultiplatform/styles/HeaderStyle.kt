package br.com.jaquesprojetos.blogmultiplatform.styles

import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import com.varabyte.kobweb.compose.css.CSSTransition
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.transition
import com.varabyte.kobweb.silk.components.style.ComponentStyle
import com.varabyte.kobweb.silk.components.style.anyLink
import com.varabyte.kobweb.silk.components.style.hover
import org.jetbrains.compose.web.css.ms

val CategoryItemStyle by ComponentStyle {
    base {
        Modifier
            .color(Theme.White.rgb)
            .transition(CSSTransition(
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