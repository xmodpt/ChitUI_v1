package com.chitui.client.data.model

import com.google.gson.JsonObject

data class Plugin(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val enabled: Boolean,
    val hasSettings: Boolean = false
)

data class PluginListResponse(
    val plugins: List<Plugin>
)

// GPIO Relay Plugin
data class RelayState(
    val relay1: Boolean = false,
    val relay2: Boolean = false,
    val relay3: Boolean = false
)

data class RelayConfig(
    val relay1Name: String = "Relay 1",
    val relay2Name: String = "Relay 2",
    val relay3Name: String = "Relay 3",
    val gpio1: Int = 17,
    val gpio2: Int = 27,
    val gpio3: Int = 22
)

// IP Camera Plugin
data class IPCamera(
    val id: String,
    val name: String,
    val url: String,
    val protocol: String, // rtsp, http, auto
    val enabled: Boolean = true
)

data class IPCameraListResponse(
    val cameras: List<IPCamera>
)

// RPi Stats Plugin
data class SystemInfo(
    val hostname: String,
    val model: String,
    val os: String,
    val pythonVersion: String,
    val cpuCores: Int
)

data class SystemStats(
    val cpuPercent: Float,
    val cpuPerCore: List<Float>,
    val cpuFreq: Float,
    val cpuTemp: Float?,
    val memoryPercent: Float,
    val memoryUsed: Long,
    val memoryTotal: Long,
    val diskPercent: Float,
    val diskUsed: Long,
    val diskTotal: Long,
    val networkSent: Long,
    val networkRecv: Long
)

// Terminal Plugin
data class TerminalMessage(
    val timestamp: Long,
    val type: String, // system_command, print_command, print_status, system_error
    val message: String
)
