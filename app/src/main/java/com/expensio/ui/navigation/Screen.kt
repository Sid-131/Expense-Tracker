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
}
