package com.chitui.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chitui.android.ui.*
import com.chitui.android.ui.theme.ChitUITheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val app: ChitUIApplication
        get() = application as ChitUIApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ChitUITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChitUIApp(app)
                }
            }
        }
    }
}

@Composable
fun ChitUIApp(app: ChitUIApplication) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Check if user is already logged in
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val isLoggedIn = app.preferencesManager.isLoggedIn().first()
        val serverUrl = app.preferencesManager.getServerUrl().first()

        startDestination = if (isLoggedIn && serverUrl != null) {
            // Initialize connection
            scope.launch {
                app.repository.connectSocketIO(serverUrl)
            }
            "printers"
        } else {
            "login"
        }
    }

    if (startDestination != null) {
        NavHost(
            navController = navController,
            startDestination = startDestination!!
        ) {
            composable("login") {
                val viewModel: LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(app.repository, app.preferencesManager)
                )
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate("printers") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable("printers") {
                val viewModel: PrintersViewModel = viewModel(
                    factory = PrintersViewModelFactory(app.repository)
                )
                PrintersScreen(
                    viewModel = viewModel,
                    onPrinterClick = { printerId ->
                        navController.navigate("printer/$printerId")
                    },
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = "printer/{printerId}",
                arguments = listOf(navArgument("printerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val printerId = backStackEntry.arguments?.getString("printerId") ?: return@composable
                val viewModel: PrinterDetailViewModel = viewModel(
                    key = printerId,
                    factory = PrinterDetailViewModelFactory(app.repository, printerId)
                )
                PrinterDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
