package br.com.jaquesprojetos.blogmultiplatform.styles

import com.varabyte.kobweb.compose.css.Transition
import com.varabyte.kobweb.compose.css.TransitionProperty
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.scale
import com.varabyte.kobweb.compose.ui.modifiers.transition
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.selectors.hover
import org.jetbrains.compose.web.css.ms
import org.jetbrains.compose.web.css.percent

val PostPreViewStyle = CssStyle {
    base {
        Modifier
            .scale(100.percent)
            .transition(
                Transition.of(
                    property = TransitionProperty.All,
                    duration = 100.ms
                )
            )
    }

    hover {
        Modifier.scale(102.percent)
    }
}
