package com.expensio.ui.navigation

sealed class Screen(val route: String) {
    // Auth graph
    object Auth : Screen("auth")
    object Login : Screen("login")
    object Signup : Screen("signup")

    // Main graph
    object Main : Screen("main")
    object Home : Screen("home")
    object Groups : Screen("groups")
    object Personal : Screen("personal")
    object Profile : Screen("profile")

    // Group screens
    object CreateGroup : Screen("create_group")
    object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: String) = "group_detail/$groupId"
    }
    object AddMember : Screen("add_member/{groupId}") {
        fun createRoute(groupId: String) = "add_member/$groupId"
    }
}
