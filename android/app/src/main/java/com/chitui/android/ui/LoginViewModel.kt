package com.chitui.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitui.android.data.ChitUIRepository
import com.chitui.android.data.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: ChitUIRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // Load saved preferences
        viewModelScope.launch {
            combine(
                preferencesManager.getServerUrl(),
                preferencesManager.getUsername(),
                preferencesManager.getRememberMe()
            ) { url, username, rememberMe ->
                Triple(url, username, rememberMe)
            }.collect { (url, username, rememberMe) ->
                _uiState.update {
                    it.copy(
                        serverUrl = url ?: "",
                        username = username ?: "",
                        rememberMe = rememberMe
                    )
                }
            }
        }
    }

    fun onServerUrlChange(url: String) {
        _uiState.update { it.copy(serverUrl = url, error = null) }
    }

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun onRememberMeChange(rememberMe: Boolean) {
        _uiState.update { it.copy(rememberMe = rememberMe) }
    }

    fun login() {
        val state = _uiState.value

        // Validation
        if (state.serverUrl.isBlank()) {
            _uiState.update { it.copy(error = "Server URL is required") }
            return
        }

        if (state.username.isBlank()) {
            _uiState.update { it.copy(error = "Username is required") }
            return
        }

        if (state.password.isBlank()) {
            _uiState.update { it.copy(error = "Password is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.login(
                serverUrl = state.serverUrl,
                username = state.username,
                password = state.password,
                rememberMe = state.rememberMe
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Login failed"
                        )
                    }
                }
            )
        }
    }
}

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)
