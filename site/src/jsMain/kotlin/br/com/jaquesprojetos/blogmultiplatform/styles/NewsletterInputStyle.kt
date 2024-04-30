package br.com.jaquesprojetos.blogmultiplatform.styles

import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import com.varabyte.kobweb.compose.css.CSSTransition
import com.varabyte.kobweb.compose.css.TransitionProperty
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.border
import com.varabyte.kobweb.compose.ui.modifiers.outline
import com.varabyte.kobweb.compose.ui.modifiers.transition
import com.varabyte.kobweb.silk.components.style.ComponentStyle
import com.varabyte.kobweb.silk.components.style.focus
import com.varabyte.kobweb.silk.components.style.hover
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.ms
import org.jetbrains.compose.web.css.px

val NewsletterInputStyle by ComponentStyle {
    base {
        Modifier
            .outline(
                width = 0.5.px,
                style = LineStyle.Solid,
                color = Theme.DarkGray.rgb
            )
            .border(
                width = 1.px,
                style = LineStyle.Solid,
                color = Colors.Transparent
            )
            .transition(CSSTransition(property = TransitionProperty.All, duration = 300.ms))
    }

    hover {
        Modifier
            .outline(
                width = 1.px,
                style = LineStyle.Solid,
                color = Theme.Primary.rgb
            )
            .border(
                width = 1.px,
                style = LineStyle.Solid,
                color = Theme.Primary.rgb
            )
    }

    focus {
        Modifier
            .outline(
                width = 1.px,
                style = LineStyle.Solid,
                color = Theme.Primary.rgb
            )
            .border(
                width = 1.px,
                style = LineStyle.Solid,
                color = Theme.Primary.rgb
            )
    }
}


val NewsletterButtonStyle by ComponentStyle {
    base {
        Modifier
            .border(
                width = 1.px,
                style = LineStyle.Solid,
                color = Theme.Primary.rgb
            )
            .transition(CSSTransition(property = TransitionProperty.All, duration = 300.ms))
    }

    hover {
        Modifier
            .border(
                width = 1.px,
                style = LineStyle.Solid,
                color = Theme.HalfBlack.rgb
            )
            .transition(CSSTransition(property = TransitionProperty.All, duration = 300.ms))

    }

    focus {
        Modifier
            .border(
                width = 1.px,
                style = LineStyle.Solid,
                color = Theme.Primary.rgb
            )
    }
}