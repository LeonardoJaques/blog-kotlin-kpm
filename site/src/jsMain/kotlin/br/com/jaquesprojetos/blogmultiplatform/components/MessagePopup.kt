package br.com.jaquesprojetos.blogmultiplatform.components

import androidx.compose.runtime.Composable
import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.util.Constants
import com.varabyte.kobweb.compose.css.TextAlign
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.fontFamily
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.onClick
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.position
import com.varabyte.kobweb.compose.ui.modifiers.textAlign
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.modifiers.zIndex
import com.varabyte.kobweb.silk.components.text.SpanText
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.vw

@Composable
fun MessagePopup(
    message: String,
    onDialogDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(100.vw)
            .height(100.vh)
            .position(Position.Fixed)
            .zIndex(19),
        contentAlignment = Alignment.Center

    ) {
        Box(
            modifier = Modifier
                .width(140.vw)
                .height(140.vh)
                .backgroundColor(Theme.HalfBlack.rgb)
                .onClick { onDialogDismiss() },
        )
        Box(
            modifier = Modifier
                .padding(all = 24.px)
                .backgroundColor(Theme.White.rgb)
                .borderRadius(4.px)
        ) {
            SpanText(
                modifier = Modifier
                    .textAlign(TextAlign.Center)
                    .fontFamily(Constants.FONT_FAMILY)
                    .fontSize(16.px),
                text = message
            )
        }
    }
}