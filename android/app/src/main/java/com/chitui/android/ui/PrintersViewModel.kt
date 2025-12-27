package com.chitui.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitui.android.data.ChitUIRepository
import com.chitui.android.data.Printer
import com.chitui.android.data.SocketEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PrintersViewModel(
    private val repository: ChitUIRepository
) : ViewModel() {

    val printers: StateFlow<List<Printer>> = repository.printers

    private val _uiState = MutableStateFlow(PrintersUiState())
    val uiState: StateFlow<PrintersUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>(replay = 0)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        // Observe socket events
        viewModelScope.launch {
            repository.socketEvents.collect { event ->
                handleSocketEvent(event)
            }
        }

        // Observe connection state
        viewModelScope.launch {
            repository.connectionState.collect { isConnected ->
                _uiState.update { it.copy(isConnected = isConnected) }
                if (isConnected) {
                    // Request printers when connected
                    repository.requestPrinters()
                }
            }
        }

        // Initial request
        if (repository.isSocketConnected()) {
            repository.requestPrinters()
        }
    }

    private fun handleSocketEvent(event: SocketEvent) {
        when (event) {
            is SocketEvent.PrintersUpdate -> {
                repository.updatePrinters(event.printers)
                _uiState.update { it.copy(isRefreshing = false) }
            }
            is SocketEvent.PrinterStatus -> {
                repository.updatePrinterStatus(event.printerId, event.status)
            }
            is SocketEvent.PrinterError -> {
                viewModelScope.launch {
                    _toastMessage.emit("Printer error: ${event.error}")
                }
            }
            is SocketEvent.PrinterNotice -> {
                viewModelScope.launch {
                    _toastMessage.emit(event.notice)
                }
            }
            is SocketEvent.Toast -> {
                viewModelScope.launch {
                    _toastMessage.emit(event.message)
                }
            }
            is SocketEvent.Connected -> {
                _uiState.update { it.copy(isConnected = true) }
            }
            is SocketEvent.Disconnected -> {
                _uiState.update { it.copy(isConnected = false) }
            }
            else -> {}
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        repository.requestPrinters()
    }

    fun discoverPrinters() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscovering = true) }

            val result = repository.discoverPrinters()
            result.fold(
                onSuccess = {
                    // Wait for socket event to update printers
                },
                onFailure = { error ->
                    _toastMessage.emit(error.message ?: "Discovery failed")
                }
            )

            _uiState.update { it.copy(isDiscovering = false) }
        }
    }

    fun removePrinter(printerId: String) {
        viewModelScope.launch {
            val result = repository.removePrinter(printerId)
            result.fold(
                onSuccess = {
                    refresh()
                },
                onFailure = { error ->
                    _toastMessage.emit(error.message ?: "Failed to remove printer")
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}

data class PrintersUiState(
    val isConnected: Boolean = false,
    val isRefreshing: Boolean = false,
    val isDiscovering: Boolean = false
)
