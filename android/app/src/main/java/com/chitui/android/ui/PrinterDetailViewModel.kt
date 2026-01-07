package com.chitui.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitui.android.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PrinterDetailViewModel(
    private val repository: ChitUIRepository,
    private val printerId: String
) : ViewModel() {

    private val _printer = MutableStateFlow<Printer?>(null)
    val printer: StateFlow<Printer?> = _printer.asStateFlow()

    private val _files = MutableStateFlow<List<PrintFile>>(emptyList())
    val files: StateFlow<List<PrintFile>> = _files.asStateFlow()

    private val _uiState = MutableStateFlow(PrinterDetailUiState())
    val uiState: StateFlow<PrinterDetailUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>(replay = 0)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        // Observe all printers and extract this printer
        viewModelScope.launch {
            repository.printers.collect { printers ->
                _printer.value = printers.find { it.id == printerId }
            }
        }

        // Observe socket events
        viewModelScope.launch {
            repository.socketEvents.collect { event ->
                handleSocketEvent(event)
            }
        }

        // Request initial data
        refreshFiles()
    }

    private fun handleSocketEvent(event: SocketEvent) {
        when (event) {
            is SocketEvent.PrinterStatus -> {
                if (event.printerId == printerId) {
                    repository.updatePrinterStatus(printerId, event.status)
                }
            }
            is SocketEvent.PrinterError -> {
                if (event.printerId == printerId) {
                    viewModelScope.launch {
                        _toastMessage.emit("Error: ${event.error}")
                    }
                }
            }
            is SocketEvent.PrinterNotice -> {
                if (event.printerId == printerId) {
                    viewModelScope.launch {
                        _toastMessage.emit(event.notice)
                    }
                }
            }
            is SocketEvent.UploadProgress -> {
                _uiState.update { it.copy(uploadProgress = event.progress) }
            }
            is SocketEvent.Toast -> {
                viewModelScope.launch {
                    _toastMessage.emit(event.message)
                }
            }
            else -> {}
        }
    }

    fun refreshFiles() {
        repository.requestPrinterFiles(printerId)
    }

    fun startPrint(filename: String) {
        repository.startPrint(printerId, filename)
    }

    fun pausePrint() {
        repository.pausePrint(printerId)
    }

    fun resumePrint() {
        repository.resumePrint(printerId)
    }

    fun stopPrint() {
        repository.stopPrint(printerId)
    }

    fun deleteFile(filename: String) {
        viewModelScope.launch {
            _toastMessage.emit("Deleting $filename...")
            repository.deleteFile(printerId, filename)
            refreshFiles()
        }
    }

    fun uploadFile(file: java.io.File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadProgress = 0) }

            val result = repository.uploadFile(file, printerId)
            result.fold(
                onSuccess = { response ->
                    _toastMessage.emit("Upload complete: ${response.filename}")
                    refreshFiles()
                },
                onFailure = { error ->
                    _toastMessage.emit(error.message ?: "Upload failed")
                }
            )

            _uiState.update { it.copy(isUploading = false, uploadProgress = 0) }
        }
    }

    fun canStartPrint(): Boolean {
        val status = _printer.value?.status ?: return false
        return status.state == PrinterState.IDLE
    }

    fun canPausePrint(): Boolean {
        val status = _printer.value?.status ?: return false
        return status.state == PrinterState.PRINTING
    }

    fun canResumePrint(): Boolean {
        val status = _printer.value?.status ?: return false
        return status.state == PrinterState.PAUSED
    }

    fun canStopPrint(): Boolean {
        val status = _printer.value?.status ?: return false
        return status.state == PrinterState.PRINTING || status.state == PrinterState.PAUSED
    }
}

data class PrinterDetailUiState(
    val isUploading: Boolean = false,
    val uploadProgress: Int = 0
)
