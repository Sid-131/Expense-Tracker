package com.expensio.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.expensio.ui.auth.AuthViewModel
import com.expensio.ui.auth.LoginScreen
import com.expensio.ui.auth.SignupScreen
import com.expensio.ui.expenses.AddExpenseScreen
import com.expensio.ui.expenses.ExpenseDetailScreen
import com.expensio.ui.groups.AddMemberScreen
import com.expensio.ui.groups.CreateGroupScreen
import com.expensio.ui.groups.GroupDetailScreen
import com.expensio.ui.groups.GroupsScreen
import com.expensio.ui.home.HomeScreen
import com.expensio.ui.personal.PersonalScreen
import com.expensio.ui.profile.ProfileScreen

@Composable
fun AppNavGraph(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val startDestination = if (currentUser != null) Screen.Main.route else Screen.Auth.route

    val rootNavController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = startDestination
    ) {
        // Auth graph
        navigation(
            startDestination = Screen.Login.route,
            route = Screen.Auth.route
        ) {
            composable(Screen.Login.route) {
                LoginScreen(navController = rootNavController, authViewModel = authViewModel)
            }
            composable(Screen.Signup.route) {
                SignupScreen(navController = rootNavController, authViewModel = authViewModel)
            }
        }

        // Main graph with bottom navigation
        composable(Screen.Main.route) {
            MainScreen(rootNavController = rootNavController, authViewModel = authViewModel)
        }

        // Group screens (outside bottom nav)
        composable(Screen.CreateGroup.route) {
            CreateGroupScreen(navController = rootNavController)
        }
        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            GroupDetailScreen(groupId = groupId, navController = rootNavController)
        }
        composable(
            route = Screen.AddMember.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            AddMemberScreen(groupId = groupId, navController = rootNavController)
        }
        composable(
            route = Screen.AddExpense.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            AddExpenseScreen(groupId = groupId, navController = rootNavController)
        }
        composable(
            route = Screen.ExpenseDetail.route,
            arguments = listOf(
                navArgument("expenseId") { type = NavType.StringType },
                navArgument("groupId") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@composable
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            ExpenseDetailScreen(expenseId = expenseId, groupId = groupId, navController = rootNavController)
        }
    }
}

@Composable
fun MainScreen(
    rootNavController: androidx.navigation.NavController,
    authViewModel: AuthViewModel
) {
    val mainNavController = rememberNavController()
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            mainNavController.navigate(item.route) {
                                popUpTo(mainNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(text = item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = mainNavController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(authViewModel = authViewModel)
            }
            composable(Screen.Groups.route) {
                GroupsScreen(navController = rootNavController)
            }
            composable(Screen.Personal.route) {
                PersonalScreen()
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    authViewModel = authViewModel,
                    onSignOut = {
                        rootNavController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Main.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
