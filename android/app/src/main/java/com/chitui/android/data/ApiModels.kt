package com.chitui.android.data

import com.google.gson.annotations.SerializedName

// Auth
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String? = null,
    val sessionId: String? = null
)

data class ChangePasswordRequest(
    @SerializedName("old_password") val oldPassword: String,
    @SerializedName("new_password") val newPassword: String
)

// Settings
data class Settings(
    val port: Int,
    val debug: Boolean,
    @SerializedName("enable_usb_gadget") val enableUsbGadget: Boolean,
    @SerializedName("usb_auto_refresh") val usbAutoRefresh: Boolean,
    @SerializedName("usb_gadget_path") val usbGadgetPath: String?,
    @SerializedName("session_timeout") val sessionTimeout: Int
)

data class SettingsUpdateRequest(
    val port: Int? = null,
    val debug: Boolean? = null,
    @SerializedName("enable_usb_gadget") val enableUsbGadget: Boolean? = null,
    @SerializedName("usb_auto_refresh") val usbAutoRefresh: Boolean? = null,
    @SerializedName("session_timeout") val sessionTimeout: Int? = null
)

// Status
data class AppStatus(
    val version: String,
    val uptime: Long,
    @SerializedName("connected_printers") val connectedPrinters: Int,
    @SerializedName("active_sessions") val activeSessions: Int
)

// Printer Discovery
data class DiscoverRequest(
    val timeout: Int = 5
)

data class ManualPrinterRequest(
    val ip: String,
    val port: Int = 3030,
    val name: String? = null
)

// File Upload
data class UploadResponse(
    val success: Boolean,
    val message: String? = null,
    val filename: String? = null
)

// API Response wrapper
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val message: String? = null
)

// SocketIO Events
sealed class SocketEvent {
    data class PrintersUpdate(val printers: List<Printer>) : SocketEvent()
    data class PrinterStatus(val printerId: String, val status: com.chitui.android.data.PrinterStatus) : SocketEvent()
    data class PrinterError(val printerId: String, val error: String) : SocketEvent()
    data class PrinterNotice(val printerId: String, val notice: String) : SocketEvent()
    data class UploadProgress(val filename: String, val progress: Int) : SocketEvent()
    data class Toast(val message: String, val type: String) : SocketEvent()
    object RefreshPage : SocketEvent()
    object Connected : SocketEvent()
    object Disconnected : SocketEvent()
}

// Print Actions
data class PrintAction(
    val printerId: String,
    val action: Action,
    val filename: String? = null,
    val startLayer: Int = 0
) {
    enum class Action {
        START, PAUSE, RESUME, STOP
    }
}

data class FileAction(
    val printerId: String,
    val filename: String,
    val action: Action
) {
    enum class Action {
        DELETE
    }
}

data class StorageAction(
    val printerId: String,
    val action: Action
) {
    enum class Action {
        CLEAR_HISTORY, WIPE_STORAGE
    }
}
