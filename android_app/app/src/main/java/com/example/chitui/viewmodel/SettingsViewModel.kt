/**
 * SettingsViewModel - Manages app settings and server configuration
 */

package com.example.chitui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitui.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val _connectionStatus = MutableStateFlow("Not tested")
    val connectionStatus: StateFlow<String> = _connectionStatus

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    companion object {
        private const val PREFS_NAME = "chitui_settings"
        private const val KEY_SERVER_URL = "server_url"
        private const val DEFAULT_SERVER_URL = "http://192.168.1.100:8080"
    }

    fun getServerUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun saveServerUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
    }

    fun testConnection(context: Context) {
        viewModelScope.launch {
            try {
                _connectionStatus.value = "Testing..."
                _isConnected.value = false

                val api = RetrofitClient.getInstance(context)
                val response = api.getStatus()

                if (response.isSuccessful && response.body()?.success == true) {
                    _connectionStatus.value = "Connected successfully"
                    _isConnected.value = true
                } else {
                    _connectionStatus.value = "Connection failed: HTTP ${response.code()}"
                    _isConnected.value = false
                }
            } catch (e: Exception) {
                _connectionStatus.value = "Error: ${e.message}"
                _isConnected.value = false
            }
        }
    }
}
