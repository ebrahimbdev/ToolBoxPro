package com.toolbox.pro.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.toolbox.pro.core.identity.IdentityEntryPoint
import com.toolbox.pro.core.localization.LocalStrings
import com.toolbox.pro.fileshare.presentation.FileShareScreen
import com.toolbox.pro.monetization.UsageGate
import com.toolbox.pro.monetization.UsageGateEntryPoint
import com.toolbox.pro.qr.presentation.QrGeneratorScreen
import com.toolbox.pro.ui.screens.DeviceInfoScreen
import com.toolbox.pro.ui.screens.HomeScreen
import com.toolbox.pro.ui.screens.PaymentScreen
import com.toolbox.pro.ui.screens.ProfileScreen
import com.toolbox.pro.ui.screens.SpeedTestScreen
import dagger.hilt.android.EntryPointAccessors

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
    data object Payment : Screen("payment", "upgradeNow", Icons.Filled.Person, Icons.Outlined.Person)
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
    val context = LocalContext.current

    LaunchedEffect(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                IdentityEntryPoint::class.java
            ).remoteConfigRepository().registerAndHeartbeat(toolOpens = 1)
        }
    }

    // Keeps `last_seen` fresh so the admin dashboard shows devices live.
    val configRepo = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                IdentityEntryPoint::class.java
            ).remoteConfigRepository()
        }.getOrNull()
    }
    LaunchedEffect(configRepo) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            runCatching { configRepo?.ping() }
        }
    }

    fun titleFor(key: String): String = when (key) {
        "home" -> s.home
        "qrCode" -> s.qrCode
        "fileShare" -> s.fileShare
        "profile" -> s.profile
        "speedTest" -> s.speedTest
        "deviceInfo" -> s.deviceInfo
        "upgradeNow" -> s.upgradeNow
        else -> key
    }

    val usageGate: UsageGate? = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                UsageGateEntryPoint::class.java
            ).usageGate()
        }.getOrNull()
    }
    val scope = rememberCoroutineScope()

    val toolRoutes = setOf(
        Screen.QrGenerator.route,
        Screen.FileShare.route,
        Screen.SpeedTest.route,
        Screen.DeviceInfo.route
    )

    fun navigateTo(route: String) {
        if (route == Screen.Home.route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Counts one "use" per tool entry; the free grant refills itself, so the
    // entry never blocks while ads are disabled.
    fun openTool(route: String) {
        if (route !in toolRoutes) {
            navigateTo(route)
            return
        }
        scope.launch {
            usageGate?.consume()
            navigateTo(route)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                                if (toolRoutes.contains(screen.route)) {
                                    openTool(screen.route)
                                } else {
                                    navigateTo(screen.route)
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
                        onNavigate = { route -> openTool(route) }
                    )
                }
                composable(Screen.QrGenerator.route) { QrGeneratorScreen() }
                composable(Screen.FileShare.route) { FileShareScreen() }
                composable(Screen.Profile.route) { ProfileScreen(navController = navController) }
                composable(Screen.SpeedTest.route) {
                    SpeedTestScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.DeviceInfo.route) {
                    DeviceInfoScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.Payment.route) { PaymentScreen(navController = navController) }
            }
        }
    }
}
