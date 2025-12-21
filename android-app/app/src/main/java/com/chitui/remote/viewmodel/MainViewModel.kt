package com.chitui.remote.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chitui.remote.data.api.ApiClient
import com.chitui.remote.data.models.*
import com.chitui.remote.data.preferences.PreferencesManager
import com.chitui.remote.data.websocket.SocketManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val socketManager = SocketManager.getInstance()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _printers = MutableStateFlow<Map<String, Printer>>(emptyMap())
    val printers: StateFlow<Map<String, Printer>> = _printers.asStateFlow()

    private val _serverUrl = MutableStateFlow<String>("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadSavedSettings()
        observeSocketConnection()
        observePrinters()
    }

    private fun loadSavedSettings() {
        viewModelScope.launch {
            preferencesManager.serverUrl.collect { url ->
                url?.let {
                    _serverUrl.value = it
                }
            }
        }
    }

    private fun observeSocketConnection() {
        viewModelScope.launch {
            socketManager.connectionState.collect { state ->
                _connectionState.value = when (state) {
                    is SocketManager.ConnectionState.Connected -> ConnectionState.Connected
                    is SocketManager.ConnectionState.Connecting -> ConnectionState.Connecting
                    is SocketManager.ConnectionState.Disconnected -> ConnectionState.Disconnected
                    is SocketManager.ConnectionState.Error -> ConnectionState.Error(state.message)
                }
            }
        }
    }

    private fun observePrinters() {
        viewModelScope.launch {
            socketManager.printers.collect { printersMap ->
                _printers.value = printersMap
            }
        }
    }

    fun connect(url: String, password: String) {
        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.Connecting
                val sanitizedUrl = url.trimEnd('/')

                // Initialize API client
                ApiClient.initialize(sanitizedUrl)

                // Attempt login
                val loginResponse = ApiClient.getApi().login(LoginRequest(password))

                if (loginResponse.isSuccessful && loginResponse.body()?.success == true) {
                    _isAuthenticated.value = true

                    // Save settings
                    preferencesManager.saveServerUrl(sanitizedUrl)
                    preferencesManager.savePassword(password)

                    // Connect WebSocket
                    socketManager.connect(sanitizedUrl)

                    _errorMessage.value = null
                } else {
                    _connectionState.value = ConnectionState.Error("Authentication failed")
                    _errorMessage.value = loginResponse.body()?.message ?: "Authentication failed"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection error", e)
                _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
                _errorMessage.value = e.message ?: "Connection failed"
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            socketManager.disconnect()
            _isAuthenticated.value = false
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun startPrint(printerId: String, fileName: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getApi().startPrint(
                    printerId,
                    mapOf("filename" to fileName)
                )
                if (!response.isSuccessful) {
                    _errorMessage.value = "Failed to start print"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting print", e)
                _errorMessage.value = e.message
            }
        }
    }

    fun pausePrint(printerId: String) {
        viewModelScope.launch {
            try {
                ApiClient.getApi().pausePrint(printerId)
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing print", e)
                _errorMessage.value = e.message
            }
        }
    }

    fun resumePrint(printerId: String) {
        viewModelScope.launch {
            try {
                ApiClient.getApi().resumePrint(printerId)
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming print", e)
                _errorMessage.value = e.message
            }
        }
    }

    fun stopPrint(printerId: String) {
        viewModelScope.launch {
            try {
                ApiClient.getApi().stopPrint(printerId)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping print", e)
                _errorMessage.value = e.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
