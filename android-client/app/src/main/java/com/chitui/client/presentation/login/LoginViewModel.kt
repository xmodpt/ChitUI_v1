package com.chitui.client.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitui.client.data.repository.ChitUIRepository
import com.chitui.client.util.PreferencesManager
import com.chitui.client.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false
)

class LoginViewModel(
    private val repository: ChitUIRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        loadSavedCredentials()
    }

    private fun loadSavedCredentials() {
        viewModelScope.launch {
            val serverUrl = preferencesManager.serverUrl.first() ?: ""
            val username = preferencesManager.username.first() ?: ""
            val password = preferencesManager.password.first() ?: ""
            val rememberMe = preferencesManager.rememberMe.first()

            _uiState.value = _uiState.value.copy(
                serverUrl = serverUrl,
                username = username,
                password = password,
                rememberMe = rememberMe
            )
        }
    }

    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
    }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun updateRememberMe(remember: Boolean) {
        _uiState.value = _uiState.value.copy(rememberMe = remember)
    }

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Save server URL first
            preferencesManager.saveServerUrl(_uiState.value.serverUrl)

            when (val result = repository.login(
                username = _uiState.value.username,
                password = _uiState.value.password,
                rememberMe = _uiState.value.rememberMe
            )) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                Resource.Loading -> {
                    // Already handled
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
