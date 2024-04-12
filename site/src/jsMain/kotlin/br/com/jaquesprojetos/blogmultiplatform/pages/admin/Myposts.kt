package br.com.jaquesprojetos.blogmultiplatform.pages.admin

import androidx.compose.runtime.Composable
import br.com.jaquesprojetos.blogmultiplatform.components.AdminPageLayout
import br.com.jaquesprojetos.blogmultiplatform.util.isUserLoggedIn
import com.varabyte.kobweb.core.Page

@Page
@Composable
fun MyPostsPage() {
    isUserLoggedIn { MyPostsScreen() }
}

@Composable
fun MyPostsScreen() {
    AdminPageLayout{

    }
}