package com.toolbox.admin.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AdminNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            AdminDashboardScreen()
        }
    }
}
