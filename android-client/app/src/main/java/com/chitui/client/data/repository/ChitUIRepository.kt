package com.chitui.client.data.repository

import com.chitui.client.data.model.*
import com.chitui.client.data.remote.ChitUIApiService
import com.chitui.client.data.remote.SocketIOManager
import com.chitui.client.util.NetworkModule
import com.chitui.client.util.PreferencesManager
import com.chitui.client.util.Resource
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File

class ChitUIRepository(
    private val preferencesManager: PreferencesManager
) {
    private var apiService: ChitUIApiService? = null
    private var socketManager: SocketIOManager? = null

    private suspend fun getApiService(): ChitUIApiService {
        if (apiService == null) {
            val serverUrl = preferencesManager.serverUrl.first() ?: throw IllegalStateException("Server URL not set")
            apiService = NetworkModule.createApiService(serverUrl)
        }
        return apiService!!
    }

    private suspend fun getSocketManager(): SocketIOManager {
        if (socketManager == null) {
            val serverUrl = preferencesManager.serverUrl.first() ?: throw IllegalStateException("Server URL not set")
            socketManager = NetworkModule.createSocketIOManager(serverUrl)
        }
        return socketManager!!
    }

    // Authentication
    suspend fun login(username: String, password: String, rememberMe: Boolean): Resource<LoginResponse> {
        return try {
            val response = getApiService().login(LoginRequest(username, password))
            if (response.isSuccessful && response.body()?.success == true) {
                preferencesManager.saveCredentials(username, password, rememberMe)
                preferencesManager.setLoggedIn(true)
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.body()?.message ?: "Login failed")
            }
        } catch (e: Exception) {
            Timber.e(e, "Login error")
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun logout(): Resource<Unit> {
        return try {
            val response = getApiService().logout()
            preferencesManager.clearSession()
            NetworkModule.resetSocketIOManager()
            socketManager = null
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Logout failed")
            }
        } catch (e: Exception) {
            Timber.e(e, "Logout error")
            Resource.Error(e.message ?: "Network error")
        }
    }

    // Socket.IO Connection
    suspend fun connectSocket(): Flow<SocketIOManager.ConnectionState> {
        return getSocketManager().connect()
    }

    fun disconnectSocket() {
        socketManager?.disconnect()
    }

    // Printers
    suspend fun requestPrinters() {
        getSocketManager().requestPrinters()
    }

    suspend fun observePrinters(): Flow<List<Printer>> {
        return getSocketManager().observePrinters()
    }

    suspend fun requestPrinterInfo(printerId: String) {
        getSocketManager().requestPrinterInfo(printerId)
    }

    suspend fun observePrinterStatus(): Flow<Pair<String, PrinterStatus>> {
        return getSocketManager().observePrinterStatus()
    }

    suspend fun observePrinterAttributes(): Flow<Pair<String, PrinterAttributes>> {
        return getSocketManager().observePrinterAttributes()
    }

    suspend fun discoverPrinters(): Resource<Unit> {
        return try {
            val response = getApiService().discoverPrinters()
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Discovery failed")
            }
        } catch (e: Exception) {
            Timber.e(e, "Discover printers error")
            Resource.Error(e.message ?: "Network error")
        }
    }

    // Files
    suspend fun requestFiles(printerId: String) {
        getSocketManager().requestFiles(printerId)
    }

    suspend fun observeFileList(): Flow<Pair<String, FileListData>> {
        return getSocketManager().observeFileList()
    }

    suspend fun uploadFile(
        file: File,
        printerId: String,
        destination: String
    ): Resource<JsonObject> {
        return try {
            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val printerIdBody = printerId.toRequestBody("text/plain".toMediaTypeOrNull())
            val destinationBody = destination.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = getApiService().uploadFile(body, printerIdBody, destinationBody)
            if (response.isSuccessful) {
                Resource.Success(response.body() ?: JsonObject())
            } else {
                Resource.Error("Upload failed")
            }
        } catch (e: Exception) {
            Timber.e(e, "Upload file error")
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteFile(printerId: String, fileUrl: String) {
        getSocketManager().deleteFile(printerId, fileUrl)
    }

    // Print Actions
    suspend fun startPrint(printerId: String, fileUrl: String, startLayer: Int = 0) {
        getSocketManager().startPrint(printerId, fileUrl, startLayer)
    }

    suspend fun pausePrint(printerId: String) {
        getSocketManager().pausePrint(printerId)
    }

    suspend fun resumePrint(printerId: String) {
        getSocketManager().resumePrint(printerId)
    }

    suspend fun stopPrint(printerId: String) {
        getSocketManager().stopPrint(printerId)
    }

    // Plugins
    suspend fun getPlugins(): Resource<PluginListResponse> {
        return try {
            val response = getApiService().getPlugins()
            if (response.isSuccessful) {
                Resource.Success(response.body() ?: PluginListResponse(emptyList()))
            } else {
                Resource.Error("Failed to get plugins")
            }
        } catch (e: Exception) {
            Timber.e(e, "Get plugins error")
            Resource.Error(e.message ?: "Network error")
        }
    }

    // GPIO Relay Plugin
    suspend fun getRelayStatus(): Resource<RelayState> {
        return try {
            val response = getApiService().getRelayStatus()
            if (response.isSuccessful) {
                Resource.Success(response.body() ?: RelayState())
            } else {
                Resource.Error("Failed to get relay status")
            }
        } catch (e: Exception) {
            Timber.e(e, "Get relay status error")
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun toggleRelay(relayNum: Int): Resource<JsonObject> {
        return try {
            val response = getApiService().toggleRelay(relayNum)
            if (response.isSuccessful) {
                Resource.Success(response.body() ?: JsonObject())
            } else {
                Resource.Error("Failed to toggle relay")
            }
        } catch (e: Exception) {
            Timber.e(e, "Toggle relay error")
            Resource.Error(e.message ?: "Network error")
        }
    }

    // IP Camera Plugin
    suspend fun getIPCameras(): Resource<IPCameraListResponse> {
        return try {
            val response = getApiService().getIPCameras()
            if (response.isSuccessful) {
                Resource.Success(response.body() ?: IPCameraListResponse(emptyList()))
            } else {
                Resource.Error("Failed to get cameras")
            }
        } catch (e: Exception) {
            Timber.e(e, "Get IP cameras error")
            Resource.Error(e.message ?: "Network error")
        }
    }

    // RPi Stats Plugin
    suspend fun getSystemInfo(): Resource<SystemInfo> {
        return try {
            val response = getApiService().getSystemInfo()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Failed to get system info")
            }
        } catch (e: Exception) {
            Timber.e(e, "Get system info error")
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getSystemStats(): Resource<SystemStats> {
        return try {
            val response = getApiService().getSystemStats()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Failed to get system stats")
            }
        } catch (e: Exception) {
            Timber.e(e, "Get system stats error")
            Resource.Error(e.message ?: "Network error")
        }
    }

    // Toast Messages
    suspend fun observeToast(): Flow<SocketIOManager.ToastMessage> {
        return getSocketManager().observeToast()
    }
}
