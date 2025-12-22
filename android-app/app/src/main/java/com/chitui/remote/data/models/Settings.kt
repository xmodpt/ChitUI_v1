package com.chitui.remote.data.models

import com.google.gson.annotations.SerializedName

data class Settings(
    val printers: Map<String, PrinterConfig>?,
    @SerializedName("auto_discover")
    val autoDiscover: Boolean?,
    val auth: AuthSettings?,
    val network: NetworkSettings?
)

data class PrinterConfig(
    val ip: String,
    val name: String,
    val model: String?,
    val brand: String?,
    val enabled: Boolean = true,
    val manual: Boolean = false,
    @SerializedName("usb_device_type")
    val usbDeviceType: String? = null,
    val image: String? = null
)

data class AuthSettings(
    @SerializedName("require_password_change")
    val requirePasswordChange: Boolean?,
    @SerializedName("session_timeout")
    val sessionTimeout: Int?
)

data class NetworkSettings(
    @SerializedName("allow_external_access")
    val allowExternalAccess: Boolean?
)

data class SystemInfo(
    val cpu: CpuInfo?,
    val memory: MemoryInfo?,
    val disk: DiskInfo?,
    val temperature: Float?
)

data class CpuInfo(
    val usage: Float,
    val cores: Int
)

data class MemoryInfo(
    val total: Long,
    val used: Long,
    val free: Long,
    val percent: Float
)

data class DiskInfo(
    val total: Long,
    val used: Long,
    val free: Long,
    val percent: Float
)

data class ApiResponse(
    val success: Boolean,
    val message: String?,
    val data: Any? = null
)

data class LoginRequest(
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("require_password_change")
    val requirePasswordChange: Boolean?
)
