package com.novelforge.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.novelforge.app.ui.home.HomeScreen
import com.novelforge.app.ui.library.LibraryScreen
import com.novelforge.app.ui.writing.WritingScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Library : Screen("library")
    object Writing : Screen("writing/{novelId}") {
        fun createRoute(novelId: Long) = "writing/$novelId"
    }
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToLibrary = {
                    navController.navigate(Screen.Library.route)
                },
                onNavigateToWriting = { novelId ->
                    navController.navigate(Screen.Writing.createRoute(novelId))
                }
            )
        }
        
        composable(Screen.Library.route) {
            LibraryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToWriting = { novelId ->
                    navController.navigate(Screen.Writing.createRoute(novelId))
                }
            )
        }
        
        composable(
            route = Screen.Writing.route,
            arguments = listOf(
                navArgument("novelId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getLong("novelId") ?: 0L
            WritingScreen(
                novelId = novelId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
