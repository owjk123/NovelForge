package com.novelforge.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.novelforge.app.ui.home.HomeScreen
import com.novelforge.app.ui.library.LibraryScreen
import com.novelforge.app.ui.settings.SettingsScreen
import com.novelforge.app.ui.writing.WritingScreen

// 底部导航的屏幕
sealed class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        label = "创作",
        selectedIcon = Icons.Filled.Create,
        unselectedIcon = Icons.Outlined.Create
    )
    object Library : BottomNavItem(
        route = "library",
        label = "书架",
        selectedIcon = Icons.Filled.LibraryBooks,
        unselectedIcon = Icons.Outlined.LibraryBooks
    )
    object Settings : BottomNavItem(
        route = "settings",
        label = "设置",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

// 写作页
object WritingScreen {
    const val route = "writing"
    fun createRoute(novelId: Long) = "writing/$novelId"
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Library,
    BottomNavItem.Settings
)

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = BottomNavItem.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 创作页（首页）
        composable(BottomNavItem.Home.route) {
            HomeScreen(
                onNavigateToWriting = { novelId ->
                    navController.navigate(WritingScreen.createRoute(novelId))
                }
            )
        }
        
        // 书架页
        composable(BottomNavItem.Library.route) {
            LibraryScreen(
                onNavigateToWriting = { novelId ->
                    navController.navigate(WritingScreen.createRoute(novelId))
                }
            )
        }
        
        // 设置页
        composable(BottomNavItem.Settings.route) {
            SettingsScreen()
        }
        
        // 写作页（独立页面，没有底部导航）
        composable(
            route = "${WritingScreen.route}/{novelId}",
            arguments = listOf(
                navArgument("novelId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getLong("novelId") ?: 0L
            WritingScreen(
                novelId = novelId,
                onNavigateBack = {
                    // 从写作页返回时，回到书架Tab
                    navController.navigate(BottomNavItem.Library.route) {
                        popUpTo(BottomNavItem.Home.route)
                    }
                }
            )
        }
    }
}
