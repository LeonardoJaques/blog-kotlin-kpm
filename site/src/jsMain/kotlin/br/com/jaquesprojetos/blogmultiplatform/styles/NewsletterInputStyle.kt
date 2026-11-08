package br.com.jaquesprojetos.blogmultiplatform.styles

import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import com.varabyte.kobweb.compose.css.Transition
import com.varabyte.kobweb.compose.css.TransitionProperty
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.border
import com.varabyte.kobweb.compose.ui.modifiers.outline
import com.varabyte.kobweb.compose.ui.modifiers.transition
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.selectors.focus
import com.varabyte.kobweb.silk.style.selectors.hover
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.ms
import org.jetbrains.compose.web.css.px

val NewsletterInputStyle = CssStyle {
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
            .transition(Transition.of(property = TransitionProperty.All, duration = 300.ms))
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


val NewsletterButtonStyle = CssStyle {
    base {
        Modifier
            .border(
                width = 1.px,
                style = LineStyle.Solid,
                color = Theme.Primary.rgb
            )
            .transition(Transition.of(property = TransitionProperty.All, duration = 300.ms))
    }

    hover {
        Modifier
            .border(
                width = 1.px,
                style = LineStyle.Solid,
                color = Theme.HalfBlack.rgb
            )
            .transition(Transition.of(property = TransitionProperty.All, duration = 300.ms))

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
