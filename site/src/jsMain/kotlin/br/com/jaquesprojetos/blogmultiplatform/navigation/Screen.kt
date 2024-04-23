package br.com.jaquesprojetos.blogmultiplatform.navigation

import br.com.jaquesprojetos.blogmultiplatform.models.Constants.POST_ID_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.QUERY_PARAM
import br.com.jaquesprojetos.blogmultiplatform.models.Constants.UPDATE_PARAM

sealed class Screen(val route: String) {
    data object AdminHome : Screen(route = "/admin/")
    data object AdminLogin : Screen(route = "/admin/login")
    data object AdminCreate : Screen(route = "/admin/create"){
        fun passPostId(id: String) = "/admin/create?$POST_ID_PARAM=$id"
    }
    data object AdminMyPosts : Screen(route = "/admin/myposts"){
        fun searchByTitle(query: String) = "/admin/myposts?${QUERY_PARAM}=$query"
    }
    data object AdminSuccess : Screen(route = "/admin/success"){
        fun postUpdated() = "/admin/success?${UPDATE_PARAM}=true"
    }

}