package br.com.jaquesprojetos.blogmultiplatform.models

import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.rgb

enum class Theme(
    val hex: String,
    val rgb: CSSColorValue
) {
    Primary(
        hex = "#00A2FF",
        rgb = rgb(r=0, g=162, b=255)
    ),
    LightGray(
        hex = "#FAFAFA",
        rgb = rgb(r=250, g=250, b=255)
    ),

    White(
        hex = "#FFFFFF",
        rgb = rgb(r=255, g=255, b=255)
    ),

    Error(
        hex = "#FF2F2F",
        rgb = rgb(r=255, g=47, b=47)
    )


}