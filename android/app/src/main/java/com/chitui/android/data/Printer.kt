package com.chitui.android.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Printer(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int = 3030,
    val model: String? = null,
    val machineName: String? = null,
    val mainboardId: String? = null,
    val firmwareVersion: String? = null,
    val isDefault: Boolean = false,
    val isConnected: Boolean = false,
    val status: PrinterStatus? = null
) : Parcelable

@Parcelize
data class PrinterStatus(
    val state: PrinterState = PrinterState.IDLE,
    val currentFile: String? = null,
    val progress: Int = 0,
    val currentLayer: Int = 0,
    val totalLayers: Int = 0,
    val printTimeRemaining: Int = 0, // seconds
    val printTimeElapsed: Int = 0, // seconds
    val errorMessage: String? = null
) : Parcelable

enum class PrinterState(val displayName: String) {
    IDLE("Idle"),
    PRINTING("Printing"),
    PAUSED("Paused"),
    ERROR("Error"),
    OFFLINE("Offline"),
    UNKNOWN("Unknown")
}

@Parcelize
data class PrinterAttributes(
    val mainboardId: String,
    val machineName: String,
    val machineType: String,
    val firmwareVersion: String,
    val resolutionX: Int,
    val resolutionY: Int,
    val machineX: Float,
    val machineY: Float,
    val machineZ: Float,
    val layerHeight: Float? = null
) : Parcelable

@Parcelize
data class PrintFile(
    val filename: String,
    val path: String,
    val size: Long = 0,
    val layers: Int = 0,
    val printTime: Int = 0, // seconds
    val thumbnailUrl: String? = null,
    val isLocal: Boolean = true
) : Parcelable

data class PrintTaskDetails(
    val filename: String,
    val currentLayer: Int,
    val totalLayers: Int,
    val progress: Int,
    val printTime: Int,
    val remainingTime: Int,
    val status: String
)
