package com.chitui.client.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chitui.client.presentation.login.LoginScreen
import com.chitui.client.presentation.main.MainScreen
import com.chitui.client.util.PreferencesManager

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
    object Settings : Screen("settings")
}

@Composable
fun ChitUIApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val preferencesManager = PreferencesManager(context)
    val isLoggedIn by preferencesManager.isLoggedIn.collectAsState(initial = false)

    val startDestination = if (isLoggedIn) Screen.Main.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
