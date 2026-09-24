package com.toolbox.pro.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.toolbox.pro.core.localization.LocalStrings
import com.toolbox.pro.fileshare.presentation.FileShareScreen
import com.toolbox.pro.qr.presentation.QrGeneratorScreen
import com.toolbox.pro.ui.screens.DeviceInfoScreen
import com.toolbox.pro.ui.screens.HomeScreen
import com.toolbox.pro.ui.screens.ProfileScreen
import com.toolbox.pro.ui.screens.SpeedTestScreen

sealed class Screen(
    val route: String,
    val titleKey: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", "home", Icons.Filled.Home, Icons.Outlined.Home)
    data object QrGenerator : Screen("qr_generator", "qrCode", Icons.Filled.QrCode, Icons.Outlined.QrCode)
    data object FileShare : Screen("file_share", "fileShare", Icons.Filled.Folder, Icons.Outlined.Folder)
    data object Profile : Screen("profile", "profile", Icons.Filled.Person, Icons.Outlined.Person)
    data object SpeedTest : Screen("speed_test", "speedTest", Icons.Filled.Home, Icons.Outlined.Home)
    data object DeviceInfo : Screen("device_info", "deviceInfo", Icons.Filled.Home, Icons.Outlined.Home)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.QrGenerator,
    Screen.FileShare,
    Screen.Profile
)

@Composable
fun ToolBoxNavHost() {
    val navController = rememberNavController()
    val s = LocalStrings.current

    fun titleFor(key: String): String = when (key) {
        "home" -> s.home
        "qrCode" -> s.qrCode
        "fileShare" -> s.fileShare
        "profile" -> s.profile
        "speedTest" -> s.speedTest
        "deviceInfo" -> s.deviceInfo
        else -> key
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = titleFor(screen.titleKey)
                            )
                        },
                        label = { Text(titleFor(screen.titleKey)) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable(Screen.QrGenerator.route) { QrGeneratorScreen() }
            composable(Screen.FileShare.route) { FileShareScreen() }
            composable(Screen.Profile.route) { ProfileScreen(navController = navController) }
            composable(Screen.SpeedTest.route) { SpeedTestScreen() }
            composable(Screen.DeviceInfo.route) { DeviceInfoScreen() }
        }
    }
}
