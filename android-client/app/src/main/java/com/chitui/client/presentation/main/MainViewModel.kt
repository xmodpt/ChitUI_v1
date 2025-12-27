package com.chitui.client.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitui.client.data.model.*
import com.chitui.client.data.remote.SocketIOManager
import com.chitui.client.data.repository.ChitUIRepository
import com.chitui.client.util.PreferencesManager
import com.chitui.client.util.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

data class MainUiState(
    val isConnected: Boolean = false,
    val printers: List<Printer> = emptyList(),
    val selectedPrinter: Printer? = null,
    val printerStatus: PrinterStatus? = null,
    val printerAttributes: PrinterAttributes? = null,
    val files: List<PrintFile> = emptyList(),
    val storageInfo: FileListData? = null,
    val plugins: List<Plugin> = emptyList(),
    val toastMessage: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class MainViewModel(
    private val repository: ChitUIRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        connectToServer()
    }

    private fun connectToServer() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Connect Socket.IO
            repository.connectSocket().collect { state ->
                when (state) {
                    is SocketIOManager.ConnectionState.Connected -> {
                        _uiState.value = _uiState.value.copy(
                            isConnected = true,
                            isLoading = false
                        )
                        requestPrinters()
                        observePrinterUpdates()
                        loadPlugins()
                    }
                    is SocketIOManager.ConnectionState.Disconnected -> {
                        _uiState.value = _uiState.value.copy(
                            isConnected = false,
                            isLoading = false
                        )
                    }
                    is SocketIOManager.ConnectionState.Connecting -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is SocketIOManager.ConnectionState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isConnected = false,
                            isLoading = false,
                            error = state.message
                        )
                    }
                }
            }
        }
    }

    private fun observePrinterUpdates() {
        // Observe printer list
        viewModelScope.launch {
            repository.observePrinters().collect { printers ->
                _uiState.value = _uiState.value.copy(printers = printers)

                // Auto-select first printer if none selected
                if (_uiState.value.selectedPrinter == null && printers.isNotEmpty()) {
                    selectPrinter(printers.first())
                }
            }
        }

        // Observe printer status
        viewModelScope.launch {
            repository.observePrinterStatus().collect { (printerId, status) ->
                if (_uiState.value.selectedPrinter?.id == printerId) {
                    _uiState.value = _uiState.value.copy(printerStatus = status)
                }
            }
        }

        // Observe printer attributes
        viewModelScope.launch {
            repository.observePrinterAttributes().collect { (printerId, attributes) ->
                if (_uiState.value.selectedPrinter?.id == printerId) {
                    _uiState.value = _uiState.value.copy(printerAttributes = attributes)
                }
            }
        }

        // Observe file list
        viewModelScope.launch {
            repository.observeFileList().collect { (printerId, fileData) ->
                if (_uiState.value.selectedPrinter?.id == printerId) {
                    _uiState.value = _uiState.value.copy(
                        files = fileData.files,
                        storageInfo = fileData
                    )
                }
            }
        }

        // Observe toast messages
        viewModelScope.launch {
            repository.observeToast().collect { toast ->
                _uiState.value = _uiState.value.copy(toastMessage = toast.message)
            }
        }
    }

    fun requestPrinters() {
        viewModelScope.launch {
            repository.requestPrinters()
        }
    }

    fun discoverPrinters() {
        viewModelScope.launch {
            when (repository.discoverPrinters()) {
                is Resource.Success -> {
                    requestPrinters()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to discover printers"
                    )
                }
                Resource.Loading -> {}
            }
        }
    }

    fun selectPrinter(printer: Printer) {
        _uiState.value = _uiState.value.copy(selectedPrinter = printer)
        viewModelScope.launch {
            repository.requestPrinterInfo(printer.id)
            repository.requestFiles(printer.id)
        }
    }

    fun refreshFiles() {
        val printerId = _uiState.value.selectedPrinter?.id ?: return
        viewModelScope.launch {
            repository.requestFiles(printerId)
        }
    }

    fun deleteFile(fileUrl: String) {
        val printerId = _uiState.value.selectedPrinter?.id ?: return
        viewModelScope.launch {
            repository.deleteFile(printerId, fileUrl)
            // Wait a bit and refresh file list
            kotlinx.coroutines.delay(1000)
            repository.requestFiles(printerId)
        }
    }

    fun startPrint(fileUrl: String, startLayer: Int = 0) {
        val printerId = _uiState.value.selectedPrinter?.id ?: return
        viewModelScope.launch {
            repository.startPrint(printerId, fileUrl, startLayer)
        }
    }

    fun pausePrint() {
        val printerId = _uiState.value.selectedPrinter?.id ?: return
        viewModelScope.launch {
            repository.pausePrint(printerId)
        }
    }

    fun resumePrint() {
        val printerId = _uiState.value.selectedPrinter?.id ?: return
        viewModelScope.launch {
            repository.resumePrint(printerId)
        }
    }

    fun stopPrint() {
        val printerId = _uiState.value.selectedPrinter?.id ?: return
        viewModelScope.launch {
            repository.stopPrint(printerId)
        }
    }

    private fun loadPlugins() {
        viewModelScope.launch {
            when (val result = repository.getPlugins()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(plugins = result.data.plugins)
                }
                is Resource.Error -> {
                    Timber.e("Failed to load plugins: ${result.message}")
                }
                Resource.Loading -> {}
            }
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            repository.disconnectSocket()
            onLogoutComplete()
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnectSocket()
    }
}
