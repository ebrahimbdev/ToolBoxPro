package com.toolbox.admin.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.toolbox.admin.ui.AdminViewModel

@Composable
fun AdminNavHost(
    vm: AdminViewModel = hiltViewModel()
) {
    val auth by vm.authState.collectAsState()

    when (auth) {
        AdminViewModel.AuthState.Unknown -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        AdminViewModel.AuthState.LoggedOut -> {
            val ui by vm.ui.collectAsState()
            AdminLoginScreen(
                loading = ui.loading,
                error = ui.error,
                onLogin = { vm.login(it) }
            )
        }
        AdminViewModel.AuthState.LoggedIn -> {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "dashboard") {
                composable("dashboard") {
                    AdminDashboardScreen(
                        vm = vm,
                        onNavigate = { navController.navigate(it) },
                        onLogout = { vm.logout() }
                    )
                }
                composable("users") {
                    AdminUsersScreen(vm = vm, onBack = { navController.popBackStack() })
                }
                composable("payments") {
                    AdminPaymentsScreen(vm = vm, onBack = { navController.popBackStack() })
                }
                composable("config") {
                    AdminConfigScreen(vm = vm, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
