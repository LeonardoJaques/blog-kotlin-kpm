package br.com.jaquesprojetos.blogmultiplatform.pages.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import br.com.jaquesprojetos.blogmultiplatform.components.AdminPageLayout
import br.com.jaquesprojetos.blogmultiplatform.components.LinkPopup
import br.com.jaquesprojetos.blogmultiplatform.components.Popup
import br.com.jaquesprojetos.blogmultiplatform.models.Category
import br.com.jaquesprojetos.blogmultiplatform.models.ControlStyle
import br.com.jaquesprojetos.blogmultiplatform.models.EditorControl
import br.com.jaquesprojetos.blogmultiplatform.models.Post
import br.com.jaquesprojetos.blogmultiplatform.models.Theme
import br.com.jaquesprojetos.blogmultiplatform.navigation.Screen
import br.com.jaquesprojetos.blogmultiplatform.styles.EditorKeyStyle
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.FONT_FAMILY
import br.com.jaquesprojetos.blogmultiplatform.util.Constants.SIDE_PANEL_WIDTH
import br.com.jaquesprojetos.blogmultiplatform.util.Id
import br.com.jaquesprojetos.blogmultiplatform.util.addPost
import br.com.jaquesprojetos.blogmultiplatform.util.applyControlStyle
import br.com.jaquesprojetos.blogmultiplatform.util.applyStyle
import br.com.jaquesprojetos.blogmultiplatform.util.getEditor
import br.com.jaquesprojetos.blogmultiplatform.util.getSelectedText
import br.com.jaquesprojetos.blogmultiplatform.util.isUserLoggedIn
import br.com.jaquesprojetos.blogmultiplatform.util.noBorder
import com.varabyte.kobweb.browser.file.loadDataUrlFromDisk
import com.varabyte.kobweb.compose.css.Cursor
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.css.Resize
import com.varabyte.kobweb.compose.css.ScrollBehavior
import com.varabyte.kobweb.compose.css.Visibility
import com.varabyte.kobweb.compose.foundation.layout.Arrangement
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.classNames
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.cursor
import com.varabyte.kobweb.compose.ui.modifiers.disabled
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxHeight
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.fontFamily
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.id
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.maxHeight
import com.varabyte.kobweb.compose.ui.modifiers.maxWidth
import com.varabyte.kobweb.compose.ui.modifiers.onClick
import com.varabyte.kobweb.compose.ui.modifiers.onKeyDown
import com.varabyte.kobweb.compose.ui.modifiers.overflow
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.resize
import com.varabyte.kobweb.compose.ui.modifiers.scrollBehavior
import com.varabyte.kobweb.compose.ui.modifiers.visibility
import com.varabyte.kobweb.compose.ui.thenIf
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.silk.components.forms.Switch
import com.varabyte.kobweb.silk.components.forms.SwitchSize
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.layout.SimpleGrid
import com.varabyte.kobweb.silk.components.layout.numColumns
import com.varabyte.kobweb.silk.components.style.breakpoint.Breakpoint
import com.varabyte.kobweb.silk.components.style.toModifier
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.theme.breakpoint.rememberBreakpoint
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.jetbrains.compose.web.dom.Ul
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.get
import kotlin.js.Date


data class createPageUiStates(
    var id: String = "",
    var title: String = "",
    var subtitle: String = "",
    var thumbnail: String = "",
    var content: String = "",
    var category: Category = Category.Programming,
    var thumbnailInputDisabled: Boolean = true,
    var editorVisibility: Boolean = true,
    var main: Boolean = false,
    var popular: Boolean = false,
    var sponsored: Boolean = false,
    var popup: Boolean = false,
    var linkPopup: Boolean = false,
    var imagePopup: Boolean = false,

    )

@Page
@Composable
fun CreatePage() {
    isUserLoggedIn { CreateScreen() }
}

@Composable
fun CreateScreen() {
    val scope = rememberCoroutineScope()
    val breakpoint = rememberBreakpoint()
    var uiState by remember { mutableStateOf(createPageUiStates()) }
    val context = rememberPageContext()

    AdminPageLayout {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .margin(topBottom = 16.px)
                .padding(left = if (breakpoint > Breakpoint.MD) SIDE_PANEL_WIDTH.px else 0.px),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxSize().maxWidth(700.px),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SimpleGrid(numColumns = numColumns(base = 1, sm = 3)) {
                    Row(
                        modifier = Modifier.margin(
                            right = if (breakpoint < Breakpoint.SM) 0.px else 24.px,
                            bottom = if (breakpoint < Breakpoint.SM) 12.px else 0.px
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            modifier = Modifier.margin(right = 8.px),
                            checked = uiState.popular,
                            onCheckedChange = {
                                uiState = uiState.copy(popular = it)
                            },
                            size = SwitchSize.MD
                        )
                        SpanText(
                            modifier = Modifier
                                .fontSize(14.px)
                                .fontFamily(FONT_FAMILY)
                                .color(Theme.HalfBlack.rgb),
                            text = "Popular"
                        )
                    }
                    Row(
                        modifier = Modifier.margin(
                            right = if (breakpoint < Breakpoint.SM) 0.px else 24.px,
                            bottom = if (breakpoint < Breakpoint.SM) 12.px else 0.px
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            modifier = Modifier.margin(right = 8.px),
                            checked = uiState.main,
                            onCheckedChange = {
                                uiState = uiState.copy(main = it)
                            },
                            size = SwitchSize.MD
                        )
                        SpanText(
                            modifier = Modifier
                                .fontSize(14.px)
                                .fontFamily(FONT_FAMILY)
                                .color(Theme.HalfBlack.rgb),
                            text = "Main"
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            modifier = Modifier.margin(right = 8.px),
                            checked = uiState.sponsored,
                            onCheckedChange = {
                                uiState = uiState.copy(sponsored = it)
                            },
                            size = SwitchSize.MD
                        )
                        SpanText(
                            modifier = Modifier
                                .fontSize(14.px)
                                .fontFamily(FONT_FAMILY)
                                .color(Theme.HalfBlack.rgb),
                            text = "Sponsored"
                        )
                    }
                }
                Input(
                    type = InputType.Text,
                    attrs = Modifier
                        .id(Id.titleInput)
                        .height(54.px)
                        .margin(topBottom = 12.px)
                        .padding(leftRight = 20.px)
                        .backgroundColor(Theme.LightGray.rgb)
                        .borderRadius(4.px)
                        .noBorder()
                        .fillMaxWidth()
                        .fontFamily(FONT_FAMILY)
                        .fontSize(16.px)
                        .toAttrs {
                            attr("placeholder", "Title")
                        },
                )

                Input(
                    type = InputType.Text,
                    attrs = Modifier
                        .id(Id.subtitleInput)
                        .height(54.px)
                        .margin(bottom = 12.px)
                        .padding(leftRight = 20.px)
                        .backgroundColor(Theme.LightGray.rgb)
                        .borderRadius(4.px)
                        .noBorder()
                        .fillMaxWidth()
                        .fontFamily(FONT_FAMILY)
                        .fontSize(16.px)
                        .toAttrs {
                            attr("placeholder", "Subtitle")
                        },
                )
                CategoryDropdown(
                    selectedCategory = uiState.category,
                    onCategorySelected = { uiState = uiState.copy(category = it) }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .margin(
                            topBottom = 12.px
                        ),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Switch(
                        modifier = Modifier.margin(right = 8.px),
                        checked = !uiState.thumbnailInputDisabled,
                        onCheckedChange = {
                            uiState = uiState.copy(thumbnailInputDisabled = !it)
                        },
                        size = SwitchSize.MD
                    )
                    SpanText(
                        modifier = Modifier
                            .fontSize(14.px)
                            .fontFamily(FONT_FAMILY)
                            .color(Theme.HalfBlack.rgb),
                        text = "Paste an Image URL instead"
                    )
                }
                ThumbNailUploader(
                    thumbnail = uiState.thumbnail,
                    thumbnailInputDisabled = uiState.thumbnailInputDisabled,
                    onThumbnailSelect = { filename, file ->
                        (document.getElementById(Id.thumbnailInput) as HTMLInputElement).value =
                            filename

                        uiState = uiState.copy(thumbnail = file)
                    }
                )
                EditorControls(
                    breakpoint = breakpoint,
                    editorVisibility = uiState.editorVisibility,
                    onEditorVisibilityChange = {
                        uiState = uiState.copy(editorVisibility = !uiState.editorVisibility)
                    },
                    onLinkClick = {
                        uiState = uiState.copy(linkPopup = true)
                    },
                    onLImageClick = {
                        uiState = uiState.copy(imagePopup = true)
                    }

                )
                Editor(editorVisibility = uiState.editorVisibility)
                CreateButton(onClick = {
                    uiState = uiState.copy(
                        title = (document.getElementById(Id.titleInput) as HTMLInputElement).value,
                    )
                    uiState = uiState.copy(
                        subtitle = (document.getElementById(Id.subtitleInput) as HTMLInputElement).value,
                    )
                    uiState = uiState.copy(
                        content = (document.getElementById(Id.editor) as HTMLTextAreaElement).value,
                    )
                    if (!uiState.thumbnailInputDisabled) {
                        uiState =
                            uiState.copy(thumbnail = (document.getElementById(Id.thumbnailInput) as HTMLInputElement).value)
                    }

                    if (
                        uiState.title.isNotEmpty() &&
                        uiState.subtitle.isNotEmpty() &&
                        uiState.thumbnail.isNotEmpty() &&
                        uiState.content.isNotEmpty()
                    ) {
                        scope.launch {
                            val result = addPost(
                                Post(
                                    author = localStorage["username"].toString(),
                                    title = uiState.title,
                                    subtitle = uiState.subtitle,
                                    date = Date.now().toLong(),
                                    thumbnail = uiState.thumbnail,
                                    content = uiState.content,
                                    category = uiState.category,
                                    popular = uiState.popular,
                                    main = uiState.main,
                                    sponsored = uiState.sponsored
                                )
                            )
                            if (result) {
                                context.router.navigateTo(Screen.AdminSuccess.route)
                            }
                        }
                    } else {
                        println("Please fill all fields")
                        scope.launch {
                            uiState = uiState.copy(popup = true)
                            delay(2000)
                            uiState = uiState.copy(popup = false)
                        }
                    }
                })
            }
            if (uiState.popup) {
                Popup(
                    message = "Please fill all fields",
                    onDialogDismiss = { uiState = uiState.copy(popup = false) }
                )
            }
            if (uiState.linkPopup) {
                LinkPopup(
                    editorControl = EditorControl.Link,
                    onDialogDismiss = { uiState = uiState.copy(linkPopup = false) },
                    onAddClick = { href, title ->
                        applyStyle(
                            ControlStyle.Link(
                                selectedText = getSelectedText(),
                                href = href,
                                title = title
                            )
                        )
                    })
            }
            if (uiState.imagePopup) {
                LinkPopup(
                    editorControl = EditorControl.Image,
                    onDialogDismiss = { uiState = uiState.copy(imagePopup = false) },
                    onAddClick = { imageUrl, description ->
                        applyStyle(
                            ControlStyle.Image(
                                selectedText = getSelectedText(),
                                imageUrl = imageUrl,
                                alt = description
                            )
                        )
                    })
            }

        }
    }

}

@Composable
fun CategoryDropdown(
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit,
) {
    Box(modifier = Modifier
        .classNames("dropdown")
        .margin(topBottom = 12.px)
        .fillMaxWidth()
        .height(54.px)
        .backgroundColor(Theme.LightGray.rgb)
        .cursor(Cursor.Pointer)
        .attrsModifier {
            attr("data-bs-toggle", "dropdown")
        }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(leftRight = 20.px),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SpanText(
                text = selectedCategory.name,
                modifier = Modifier.fillMaxWidth().fontFamily(FONT_FAMILY).fontSize(16.px)
            )
            Box(modifier = Modifier.classNames("dropdown-toggle"))
        }
        Ul(
            attrs = Modifier
                .fillMaxWidth()
                .classNames("dropdown-menu")
                .toAttrs()
        ) {
            Category.entries.forEach { category ->
                Li {
                    A(attrs = Modifier
                        .classNames("dropdown-item")
                        .color(Theme.Black.rgb)
                        .fontFamily(FONT_FAMILY)
                        .fontSize(16.px)
                        .onClick { onCategorySelected(category) }
                        .toAttrs()) {
                        Text(category.name)
                    }
                }
            }
        }
    }
}

@Composable
fun ThumbNailUploader(
    thumbnail: String,
    thumbnailInputDisabled: Boolean,
    onThumbnailSelect: (String, String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .margin(bottom = 20.px)
            .height(54.px),
    ) {
        Input(
            type = InputType.Text,
            attrs = Modifier
                .id(Id.thumbnailInput)
                .margin(right = 12.px)
                .fillMaxSize()
                .padding(leftRight = 20.px)
                .backgroundColor(Theme.LightGray.rgb)
                .noBorder()
                .borderRadius(4.px)
                .fontFamily(FONT_FAMILY)
                .fontSize(16.px)
                .thenIf(
                    condition = thumbnailInputDisabled,
                    other = Modifier.disabled()
                )
                .toAttrs {
                    attr("placeholder", "Thumbnail")
                    attr("value", thumbnail)
                }
        )
        Button(
            attrs = Modifier
                .onClick {
                    document.loadDataUrlFromDisk(
                        accept = "image/png, image/jpeg",
                        onLoad = {
                            onThumbnailSelect(filename, it)
                        }
                    )
                }
                .fontFamily(FONT_FAMILY)
                .fontSize(14.px)
                .fontWeight(FontWeight.Medium)
                .fillMaxHeight()
                .padding(leftRight = 24.px)
                .backgroundColor(if (!thumbnailInputDisabled) Theme.Gray.rgb else Theme.Primary.rgb)
                .color(if (!thumbnailInputDisabled) Theme.DarkGray.rgb else Theme.White.rgb)
                .borderRadius(4.px)
                .noBorder()
                .cursor(Cursor.Pointer)
                .thenIf(
                    condition = !thumbnailInputDisabled,
                    other = Modifier.disabled()
                )
                .toAttrs()
        ) {
            SpanText("Upload")
        }
    }
}

@Composable
fun EditorControls(
    breakpoint: Breakpoint,
    editorVisibility: Boolean,
    onLinkClick: () -> Unit = {},
    onLImageClick: () -> Unit = {},
    onEditorVisibilityChange: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        SimpleGrid(
            numColumns = numColumns(base = 1, sm = 2),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .height(54.px)
                    .backgroundColor(Theme.LightGray.rgb)
                    .borderRadius(4.px)
            ) {
                EditorControl.entries.forEach {
                    EditorControlView(
                        control = it,
                        onClick = {
                            applyControlStyle(
                                editorControl = it,
                                onLinkClick = onLinkClick,
                                onImageClick = onLImageClick
                            )
                        }
                    )
                }
            }
            Box(
                contentAlignment = Alignment.CenterEnd
            ) {
                Button(
                    attrs = Modifier
                        .height(54.px)
                        .margin(topBottom = if (breakpoint < Breakpoint.SM) 12.px else 0.px)
                        .thenIf(
                            condition = breakpoint < Breakpoint.SM,
                            other = Modifier.fillMaxWidth()
                        )
                        .padding(leftRight = 24.px)
                        .borderRadius(4.px)
                        .backgroundColor(
                            if (editorVisibility) Theme.LightGray.rgb else Theme.Primary.rgb
                        )
                        .color(
                            if (editorVisibility) Theme.DarkGray.rgb else Theme.White.rgb
                        )
                        .noBorder()
                        .onClick {
                            onEditorVisibilityChange()
                            document.getElementById(Id.editorPreview)?.innerHTML = getEditor().value
                            js("hljs.highlightAll()") as Unit
                        }
                        .toAttrs()
                ) {
                    SpanText(
                        text = "Preview",
                        modifier = Modifier
                            .fontFamily(FONT_FAMILY)
                            .fontSize(14.px)
                            .fontWeight(FontWeight.Medium)
                    )
                }
            }
        }
    }
}

@Composable
fun EditorControlView(
    control: EditorControl,
    onClick: () -> Unit,

    ) {
    Box(
        modifier = EditorKeyStyle.toModifier()
            .fillMaxHeight()
            .padding(leftRight = 12.px)
            .borderRadius(4.px)
            .cursor(Cursor.Pointer)
            .onClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            src = control.icon,
            description = "${control.name} icon",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun Editor(editorVisibility: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        TextArea(
            attrs = Modifier
                .id(Id.editor)
                .fontFamily(FONT_FAMILY)
                .fontSize(16.px)
                .fillMaxWidth()
                .height(400.px)
                .maxHeight(400.px)
                .margin(top = 8.px)
                .padding(all = 20.px)
                .backgroundColor(Theme.LightGray.rgb)
                .borderRadius(4.px)
                .noBorder()
                .resize(Resize.None)
                .visibility(if (editorVisibility) Visibility.Visible else Visibility.Hidden)
                .onKeyDown {
                    if (it.code == "Enter" && it.shiftKey) {
                        applyStyle(
                            controlStyle = ControlStyle.Break(
                                selectedText = getSelectedText()
                            )
                        )
                    }
                }
                .toAttrs {
                    attr("placeholder", "Write your article here...")
                }
        )
        Div(
            attrs = Modifier
                .id(Id.editorPreview)
                .fillMaxWidth()
                .height(400.px)
                .maxHeight(400.px)
                .margin(top = 8.px)
                .padding(all = 20.px)
                .backgroundColor(Theme.LightGray.rgb)
                .borderRadius(4.px)
                .noBorder()
                .visibility(if (editorVisibility) Visibility.Hidden else Visibility.Visible)
                .overflow(Overflow.Auto)
                .scrollBehavior(ScrollBehavior.Smooth)
                .toAttrs()
        )
    }
}

@Composable
fun CreateButton(onClick: () -> Unit) {
    Button(
        attrs = Modifier
            .onClick { onClick() }
            .fillMaxWidth()
            .height(54.px)
            .margin(top = 24.px)
            .padding(leftRight = 24.px)
            .borderRadius(4.px)
            .backgroundColor(Theme.Primary.rgb)
            .color(Theme.White.rgb)
            .noBorder()
            .fontFamily(FONT_FAMILY)
            .fontSize(16.px)
            .cursor(Cursor.Pointer)
            .toAttrs()
    ) {
        SpanText("Create")
    }
}