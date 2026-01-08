/**
 * Login Activity for ChitUI Android App
 *
 * This activity handles user authentication and obtains JWT token
 *
 * Features:
 * - Password input
 * - Server URL configuration
 * - Remember credentials
 * - Error handling
 * - Auto-login if token exists
 */

package com.example.chitui.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.chitui.api.LoginRequest
import com.example.chitui.api.RetrofitClient
import com.example.chitui.api.TokenManager
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(this)

        // Check if already logged in
        if (tokenManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        setContent {
            MaterialTheme {
                LoginScreen(
                    onLoginClick = { password, serverUrl ->
                        performLogin(password, serverUrl)
                    }
                )
            }
        }
    }

    private fun performLogin(password: String, serverUrl: String) {
        lifecycleScope.launch {
            try {
                // Update server URL if changed
                if (serverUrl.isNotBlank()) {
                    RetrofitClient.updateBaseUrl(serverUrl, this@LoginActivity)
                }

                val api = RetrofitClient.getInstance(this@LoginActivity)
                val response = api.login(LoginRequest(password))

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.token != null) {
                        // Save token
                        tokenManager.saveToken(body.token, body.expires_in ?: 2592000)

                        Toast.makeText(
                            this@LoginActivity,
                            "Login successful!",
                            Toast.LENGTH_SHORT
                        ).show()

                        navigateToMain()
                    } else {
                        showError(body?.message ?: "Login failed")
                    }
                } else {
                    showError("HTTP ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                showError("Network error: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@Composable
fun LoginScreen(
    onLoginClick: (password: String, serverUrl: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("http://192.168.1.100:8080") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo/title
            Text(
                text = "ChitUI Remote",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Server URL input
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Server URL") },
                placeholder = { Text("http://192.168.1.100:8080") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Password input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                singleLine = true,
                visualTransformation = if (showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (showPassword) {
                                "Hide password"
                            } else {
                                "Show password"
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            // Login button
            Button(
                onClick = {
                    if (password.isNotBlank()) {
                        isLoading = true
                        onLoginClick(password, serverUrl)
                    }
                },
                enabled = !isLoading && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Login")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Help text
            Text(
                text = "Default password: admin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
