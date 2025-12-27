package com.chitui.android.data

import android.util.Log
import com.chitui.android.api.ChitUIApi
import com.chitui.android.api.RetrofitClient
import com.chitui.android.api.SocketIOClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ChitUIRepository(
    private val preferencesManager: PreferencesManager
) {

    private val api: ChitUIApi
        get() = RetrofitClient.getApi()

    private val socketClient = SocketIOClient()

    private val _printers = MutableStateFlow<List<Printer>>(emptyList())
    val printers: StateFlow<List<Printer>> = _printers.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // SocketIO events
    val socketEvents: Flow<SocketEvent> = socketClient.events
    val connectionState: Flow<Boolean> = socketClient.connectionState

    // Authentication
    suspend fun login(serverUrl: String, username: String, password: String, rememberMe: Boolean): Result<Unit> {
        return try {
            // Initialize Retrofit with server URL
            RetrofitClient.initialize(serverUrl)

            val response = api.login(LoginRequest(username, password))

            if (response.isSuccessful && response.body()?.success == true) {
                // Save credentials
                preferencesManager.saveServerUrl(serverUrl)
                preferencesManager.saveCredentials(username, rememberMe)
                preferencesManager.setLoggedIn(true)

                // Connect SocketIO
                connectSocketIO(serverUrl)

                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Login failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            api.logout()
            disconnectSocketIO()
            RetrofitClient.clearSession()
            preferencesManager.setLoggedIn(false)
            _printers.value = emptyList()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Logout error", e)
            Result.failure(e)
        }
    }

    // SocketIO
    fun connectSocketIO(serverUrl: String) {
        socketClient.connect(serverUrl)
    }

    fun disconnectSocketIO() {
        socketClient.disconnect()
    }

    fun isSocketConnected(): Boolean = socketClient.isConnected()

    // Printers
    fun requestPrinters() {
        socketClient.requestPrinters()
    }

    suspend fun discoverPrinters(): Result<Unit> {
        return try {
            val response = api.discoverPrinters()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Discovery failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Discover printers error", e)
            Result.failure(e)
        }
    }

    suspend fun addPrinterManually(ip: String, port: Int, name: String?): Result<Unit> {
        return try {
            val response = api.addPrinterManually(ManualPrinterRequest(ip, port, name))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to add printer"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Add printer error", e)
            Result.failure(e)
        }
    }

    suspend fun removePrinter(printerId: String): Result<Unit> {
        return try {
            val response = api.removePrinter(printerId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to remove printer"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Remove printer error", e)
            Result.failure(e)
        }
    }

    // Print Actions
    fun startPrint(printerId: String, filename: String, startLayer: Int = 0) {
        socketClient.startPrint(printerId, filename, startLayer)
    }

    fun pausePrint(printerId: String) {
        socketClient.pausePrint(printerId)
    }

    fun resumePrint(printerId: String) {
        socketClient.resumePrint(printerId)
    }

    fun stopPrint(printerId: String) {
        socketClient.stopPrint(printerId)
    }

    // File Operations
    fun deleteFile(printerId: String, filename: String) {
        socketClient.deleteFile(printerId, filename)
    }

    fun requestPrinterFiles(printerId: String) {
        socketClient.requestPrinterFiles(printerId)
    }

    suspend fun uploadFile(file: File, printerId: String? = null): Result<UploadResponse> {
        return try {
            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = api.uploadFile(multipartBody, printerId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Upload failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload file error", e)
            Result.failure(e)
        }
    }

    // Settings
    suspend fun getSettings(): Result<Settings> {
        return try {
            val response = api.getSettings()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get settings"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get settings error", e)
            Result.failure(e)
        }
    }

    suspend fun updateSettings(settings: SettingsUpdateRequest): Result<Unit> {
        return try {
            val response = api.updateSettings(settings)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update settings"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update settings error", e)
            Result.failure(e)
        }
    }

    // Update local printer list
    fun updatePrinters(printers: List<Printer>) {
        _printers.value = printers
    }

    fun updatePrinterStatus(printerId: String, status: PrinterStatus) {
        _printers.value = _printers.value.map { printer ->
            if (printer.id == printerId) {
                printer.copy(status = status)
            } else {
                printer
            }
        }
    }

    companion object {
        private const val TAG = "ChitUIRepository"
    }
}
