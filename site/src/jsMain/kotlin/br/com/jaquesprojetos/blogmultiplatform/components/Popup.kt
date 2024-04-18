package br.com.jaquesprojetos.blogmultiplatform.components

import androidx.compose.runtime.Composable
import br.com.jaquesprojetos.blogmultiplatform.models.EditorControl
import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.FONT_FAMILY
import br.com.jaquesprojetos.blogmultiplatform.util.Id
import br.com.jaquesprojetos.blogmultiplatform.util.noBorder
import com.varabyte.kobweb.compose.css.TextAlign
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.fontFamily
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.id
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.onClick
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.position
import com.varabyte.kobweb.compose.ui.modifiers.textAlign
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.modifiers.zIndex
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.components.text.SpanText
import kotlinx.browser.document
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.vw
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Input
import org.w3c.dom.HTMLInputElement

@Composable
fun Popup(
    message: String,
    onDialogDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier.width(100.vw).height(100.vh).position(Position.Fixed).zIndex(19),
        contentAlignment = Alignment.Center

    ) {
        Box(
            modifier = Modifier.width(140.vw).height(140.vh).backgroundColor(Theme.HalfBlack.rgb)
                .onClick { onDialogDismiss() },
        )
        Box(
            modifier = Modifier.padding(all = 24.px).backgroundColor(Theme.White.rgb)
                .borderRadius(4.px)
        ) {
            SpanText(
                modifier = Modifier.textAlign(TextAlign.Center).fontFamily(FONT_FAMILY)
                    .fontSize(16.px), text = message
            )
        }
    }
}

@Composable
fun LinkPopup(
    editorControl: EditorControl,
    onDialogDismiss: () -> Unit,
    onAddClick: (String, String) -> Unit,
) {
    Box(
        modifier = Modifier.width(100.vw).height(100.vh).position(Position.Fixed).zIndex(19),
        contentAlignment = Alignment.Center

    ) {
        Box(
            modifier = Modifier.width(140.vw).height(140.vh).backgroundColor(Theme.HalfBlack.rgb)
                .onClick { onDialogDismiss() },
        )
        Column(
            modifier = Modifier
                .width(500.px)
                .padding(all = 24.px)
                .backgroundColor(Theme.White.rgb)
                .borderRadius(4.px)
        ) {
            Input(
                type = InputType.Text,
                attrs = Modifier
                    .id(Id.linkHrefInput)
                    .fillMaxWidth()
                    .height(54.px)
                    .padding(leftRight = 20.px)
                    .margin(bottom = 12.px)
                    .fontFamily(FONT_FAMILY)
                    .fontSize(14.px)
                    .noBorder()
                    .borderRadius(r = 4.px)
                    .backgroundColor(Theme.LightGray.rgb)
                    .toAttrs {
                        attr("placeholder", if (editorControl == EditorControl.Link) "Href" else "Image URL")
                    }
            )
            Input(
                type = InputType.Text,
                attrs = Modifier
                    .id(Id.linkTitleInput)
                    .fillMaxWidth()
                    .height(54.px)
                    .margin(bottom = 20.px)
                    .padding(leftRight = 20.px)
                    .fontFamily(FONT_FAMILY)
                    .fontSize(14.px)
                    .noBorder()
                    .borderRadius(r = 4.px)
                    .backgroundColor(Theme.LightGray.rgb)
                    .toAttrs {
                        attr("placeholder", if (editorControl == EditorControl.Link) "Title" else "Description")
                    }
            )

            Button(
                attrs = Modifier
                    .onClick {
                        val href =
                            (document.getElementById(Id.linkHrefInput) as HTMLInputElement).value
                        val title =
                            (document.getElementById(Id.linkTitleInput) as HTMLInputElement).value
                        onAddClick(href, title)
                        onDialogDismiss()

                    }
                    .fillMaxWidth()
                    .height(54.px)
                    .backgroundColor(Theme.Primary.rgb)
                    .fontFamily(FONT_FAMILY)
                    .fontSize(14.px)
                    .color(Theme.White.rgb)
                    .borderRadius(r = 4.px)
                    .noBorder()
                    .toAttrs()
            ) {
                SpanText(text = "Add")
            }
        }
    }
}