package br.com.jaquesprojetos.blogmultiplatform.util
object Constants {
    const val FONT_FAMILY = "Roboto"
    const val SIDE_PANEL_WIDTH = 250
    const val PAGE_WIDTH = 100
    const val PAGE_WIDTH_EX = 1920
    const val COLLAPSED_PANEL_HEIGHT = 100
    private const val API_KEY= "a83b069520da412c9dbfc531674a446b"
    const val HUMOR_API_URL =  "https://api.humorapi.com/jokes/random?api-key=${API_KEY}&max-length=180"
    const val HEADER_HEIGHT = 100



}

object Res {
    object Image {
        const val logo = "/logo.svg"
        const val logoHome = "logo.svg"
        const val lagth = "/lagth.png"
    }

    object FontFamily {
        object Roboto {
            const val regular = "fonts/Roboto-Regular.ttf"
            const val bold = "fonts/Roboto-Bold.ttf"
            const val medium = "fonts/Roboto-Medium.ttf"
            const val light = "fonts/Roboto-Light.ttf"
        }

    }

    object Icon {
        const val bold = "/bold.svg"
        const val italic = "/italic.svg"
        const val link = "/link.svg"
        const val quote = "/quote.svg"
        const val title = "/title.svg"
        const val subtitle = "/subtitle.svg"
        const val image = "/image.svg"
        const val code = "/code.svg"
        const val checkmark = "/checkmark.svg"
    }
    object PathIcon {
        const val home =
            "M3 12.5215L5 10.5215M5 10.5215L12 3.52148L19 10.5215M5 10.5215V20.5215C5 20.7867 5.10536 21.0411 5.29289 21.2286C5.48043 21.4161 5.73478 21.5215 6 21.5215H9M19 10.5215L21 12.5215M19 10.5215V20.5215C19 20.7867 18.8946 21.0411 18.7071 21.2286C18.5196 21.4161 18.2652 21.5215 18 21.5215H15M9 21.5215C9.26522 21.5215 9.51957 21.4161 9.70711 21.2286C9.89464 21.0411 10 20.7867 10 20.5215V16.5215C10 16.2563 10.1054 16.0019 10.2929 15.8144C10.4804 15.6268 10.7348 15.5215 11 15.5215H13C13.2652 15.5215 13.5196 15.6268 13.7071 15.8144C13.8946 16.0019 14 16.2563 14 16.5215V20.5215C14 20.7867 14.1054 21.0411 14.2929 21.2286C14.4804 21.4161 14.7348 21.5215 15 21.5215M9 21.5215H15"
        const val create =
            "M12 9.52148V12.5215M12 12.5215V15.5215M12 12.5215H15M12 12.5215H9M21 12.5215C21 13.7034 20.7672 14.8737 20.3149 15.9656C19.8626 17.0576 19.1997 18.0497 18.364 18.8854C17.5282 19.7212 16.5361 20.3841 15.4442 20.8364C14.3522 21.2887 13.1819 21.5215 12 21.5215C10.8181 21.5215 9.64778 21.2887 8.55585 20.8364C7.46392 20.3841 6.47177 19.7212 5.63604 18.8854C4.80031 18.0497 4.13738 17.0576 3.68508 15.9656C3.23279 14.8737 3 13.7034 3 12.5215C3 10.1345 3.94821 7.84535 5.63604 6.15752C7.32387 4.4697 9.61305 3.52148 12 3.52148C14.3869 3.52148 16.6761 4.4697 18.364 6.15752C20.0518 7.84535 21 10.1345 21 12.5215Z"
        const val posts =
            "M9 5.52148H7C6.46957 5.52148 5.96086 5.7322 5.58579 6.10727C5.21071 6.48234 5 6.99105 5 7.52148V19.5215C5 20.0519 5.21071 20.5606 5.58579 20.9357C5.96086 21.3108 6.46957 21.5215 7 21.5215H17C17.5304 21.5215 18.0391 21.3108 18.4142 20.9357C18.7893 20.5606 19 20.0519 19 19.5215V7.52148C19 6.99105 18.7893 6.48234 18.4142 6.10727C18.0391 5.7322 17.5304 5.52148 17 5.52148H15M9 5.52148C9 6.05192 9.21071 6.56063 9.58579 6.9357C9.96086 7.31077 10.4696 7.52148 11 7.52148H13C13.5304 7.52148 14.0391 7.31077 14.4142 6.9357C14.7893 6.56063 15 6.05192 15 5.52148M9 5.52148C9 4.99105 9.21071 4.48234 9.58579 4.10727C9.96086 3.7322 10.4696 3.52148 11 3.52148H13C13.5304 3.52148 14.0391 3.7322 14.4142 4.10727C14.7893 4.48234 15 4.99105 15 5.52148M12 12.5215H15M12 16.5215H15M9 12.5215H9.01M9 16.5215H9.01"
        const val logout =
            "M11 16.5215L7 12.5215M7 12.5215L11 8.52148M7 12.5215H21M16 16.5215V17.5215C16 18.3171 15.6839 19.0802 15.1213 19.6428C14.5587 20.2054 13.7956 20.5215 13 20.5215H6C5.20435 20.5215 4.44129 20.2054 3.87868 19.6428C3.31607 19.0802 3 18.3171 3 17.5215V7.52148C3 6.72583 3.31607 5.96277 3.87868 5.40016C4.44129 4.83755 5.20435 4.52148 6 4.52148H13C13.7956 4.52148 14.5587 4.83755 15.1213 5.40016C15.6839 5.96277 16 6.72583 16 7.52148V8.52148"

    }
}

object Id {
    const val usernameInput = "usernameInput"
    const val passwordInput = "passwordInput"
    const val svgParent = "svgParent"
    const val vectorIcon = "vectorIcon"
    const val navigationText = "navigationText"
    const val editor = "editor"
    const val editorPreview = "editorPreview"
    const val titleInput = "titleInput"
    const val subtitleInput = "subtitleInput"
    const val thumbnailInput = "thumbnailInput"
    const val linkHrefInput = "linkHrefInput"
    const val linkTitleInput = "linkTitleInput"
    const val adminSearchBar = "adminSearchBar"
    const val emailInput = "emailInput"




}

//TODO: Add Path to the API