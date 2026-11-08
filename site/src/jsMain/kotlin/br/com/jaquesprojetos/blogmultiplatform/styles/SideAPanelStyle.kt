package br.com.jaquesprojetos.blogmultiplatform.styles

import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.util.Id
import com.varabyte.kobweb.compose.css.Transition
import com.varabyte.kobweb.compose.css.TransitionProperty
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.transition
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.silk.style.CssStyle
import org.jetbrains.compose.web.css.ms

val NavigationItemStyle = CssStyle {
    cssRule(
        "> #${Id.svgParent} > #${Id.vectorIcon}"
    ) {
        Modifier
            .styleModifier {
                property("stroke", Theme.White.hex)
            }
    }
    cssRule(
        ":hover > #${Id.svgParent} > #${Id.vectorIcon}"
    ) {
        Modifier
            .transition(
                Transition.of(
                    property = TransitionProperty.All,
                    duration = 300.ms,
                )
            )
            .styleModifier {
                property("stroke", Theme.Primary.hex)
            }
    }

    cssRule("> #${Id.navigationText}") {
        Modifier
            .transition(
                Transition.of(
                    property = TransitionProperty.All,
                    duration = 300.ms,
                )
            )
            .color(Theme.White.rgb)
    }

    cssRule(":hover > #${Id.navigationText}") {
        Modifier.color(Theme.Primary.rgb)
    }
}
