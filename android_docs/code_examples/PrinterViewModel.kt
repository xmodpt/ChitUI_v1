/**
 * ViewModel for Printer Management
 *
 * This ViewModel manages printer data and provides a clean interface for the UI layer.
 * It handles:
 * - Loading printer list
 * - Real-time updates via Socket.IO
 * - Print control actions
 * - Error handling
 *
 * Usage in Activity/Fragment:
 *   val viewModel: PrinterViewModel by viewModels()
 *   viewModel.printers.observe(this) { printers ->
 *       // Update UI with printer list
 *   }
 */

package com.example.chitui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.chitui.api.ChitUIApi
import com.example.chitui.api.Printer
import com.example.chitui.api.RetrofitClient
import com.example.chitui.socket.ChitUISocketClient
import kotlinx.coroutines.launch

class PrinterViewModel(application: Application) : AndroidViewModel(application) {

    private val api: ChitUIApi = RetrofitClient.getInstance(application)
    private val socketClient = ChitUISocketClient(application)

    // LiveData for UI observation
    private val _printers = MutableLiveData<List<Printer>>()
    val printers: LiveData<List<Printer>> = _printers

    private val _selectedPrinter = MutableLiveData<Printer?>()
    val selectedPrinter: LiveData<Printer?> = _selectedPrinter

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    companion object {
        private const val TAG = "PrinterViewModel"
    }

    init {
        setupSocketListeners()
        connectSocket()
    }

    /**
     * Setup Socket.IO listeners for real-time updates
     */
    private fun setupSocketListeners() {
        socketClient.setOnConnectedListener {
            _isConnected.postValue(true)
            Log.d(TAG, "Socket connected - requesting printer update")
            socketClient.requestPrinterUpdate()
        }

        socketClient.setOnDisconnectedListener {
            _isConnected.postValue(false)
            Log.d(TAG, "Socket disconnected")
        }

        socketClient.setOnPrinterUpdateListener { printerList ->
            _printers.postValue(printerList)
            Log.d(TAG, "Received ${printerList.size} printers from socket")
        }

        socketClient.setOnPrinterInfoListener { printer ->
            // Update selected printer if it matches
            if (_selectedPrinter.value?.id == printer.id) {
                _selectedPrinter.postValue(printer)
            }
        }

        socketClient.setOnErrorListener { errorMessage ->
            _error.postValue(errorMessage)
            Log.e(TAG, "Socket error: $errorMessage")
        }
    }

    /**
     * Connect to Socket.IO server
     */
    fun connectSocket() {
        socketClient.connect()
    }

    /**
     * Disconnect from Socket.IO server
     */
    fun disconnectSocket() {
        socketClient.disconnect()
    }

    /**
     * Load printers from API (REST call as fallback)
     */
    fun loadPrinters() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val response = api.getPrinters()

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        _printers.value = body.printers ?: emptyList()
                        Log.d(TAG, "Loaded ${body.printers?.size} printers from API")
                    } else {
                        _error.value = body?.message ?: "Failed to load printers"
                    }
                } else {
                    _error.value = "HTTP ${response.code()}: ${response.message()}"
                    Log.e(TAG, "Failed to load printers: ${response.code()}")
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                Log.e(TAG, "Error loading printers", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load detailed info for specific printer
     *
     * @param printerId Printer ID to query
     */
    fun loadPrinterInfo(printerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val response = api.getPrinterInfo(printerId)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        _selectedPrinter.value = body.printer
                        Log.d(TAG, "Loaded info for printer: $printerId")
                    } else {
                        _error.value = body?.message ?: "Failed to load printer info"
                    }
                } else {
                    _error.value = "HTTP ${response.code()}: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
                Log.e(TAG, "Error loading printer info", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Start print job
     *
     * @param printerId Printer ID
     * @param fileName G-code file name to print
     */
    fun startPrint(printerId: String, fileName: String) {
        socketClient.sendPrintAction(printerId, "print", fileName)
        Log.d(TAG, "Starting print: $fileName on printer $printerId")
    }

    /**
     * Pause current print
     *
     * @param printerId Printer ID
     */
    fun pausePrint(printerId: String) {
        socketClient.sendPrintAction(printerId, "pause")
        Log.d(TAG, "Pausing print on printer $printerId")
    }

    /**
     * Resume paused print
     *
     * @param printerId Printer ID
     */
    fun resumePrint(printerId: String) {
        socketClient.sendPrintAction(printerId, "resume")
        Log.d(TAG, "Resuming print on printer $printerId")
    }

    /**
     * Stop current print
     *
     * @param printerId Printer ID
     */
    fun stopPrint(printerId: String) {
        socketClient.sendPrintAction(printerId, "stop")
        Log.d(TAG, "Stopping print on printer $printerId")
    }

    /**
     * Select a printer for detailed view
     *
     * @param printer Printer to select
     */
    fun selectPrinter(printer: Printer) {
        _selectedPrinter.value = printer
        // Request updated info via socket
        socketClient.requestPrinterInfo(printer.id)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Cleanup when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        socketClient.disconnect()
        Log.d(TAG, "ViewModel cleared - socket disconnected")
    }
}
