/**
 * MainActivity - Main application screen with bottom navigation
 *
 * This activity hosts the main screens:
 * - Printers List
 * - Printer Details
 * - Settings
 */

package com.example.chitui.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.chitui.api.TokenManager
import com.example.chitui.viewmodel.PrinterViewModel

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Printers : Screen("printers", "Printers", Icons.Default.Print)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object PrinterDetail : Screen("printer/{printerId}", "Printer Detail", Icons.Default.Info)
}

class MainActivity : ComponentActivity() {

    private val viewModel: PrinterViewModel by viewModels()
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(this)

        // Check if logged in
        if (!tokenManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            MaterialTheme {
                MainScreen(viewModel)
            }
        }

        // Load initial data
        viewModel.loadPrinters()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnectSocket()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PrinterViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                listOf(Screen.Printers, Screen.Settings).forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination and save state
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
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Printers.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Printers.route) {
                PrinterListScreen(
                    viewModel = viewModel,
                    onPrinterClick = { printer ->
                        navController.navigate("printer/${printer.id}")
                    }
                )
            }

            composable(Screen.PrinterDetail.route) { backStackEntry ->
                val printerId = backStackEntry.arguments?.getString("printerId")
                if (printerId != null) {
                    PrinterDetailScreen(
                        printerId = printerId,
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onLogout = {
                        // Handle logout
                        val tokenManager = TokenManager(navController.context)
                        tokenManager.clearToken()

                        // Navigate to login
                        val intent = Intent(navController.context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        navController.context.startActivity(intent)
                    }
                )
            }
        }
    }
}
