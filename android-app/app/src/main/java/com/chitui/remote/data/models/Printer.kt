package com.chitui.remote.data.models

import com.google.gson.annotations.SerializedName

data class Printer(
    @SerializedName("connection")
    val id: String,
    val name: String,
    val model: String?,
    val brand: String?,
    val ip: String,
    val protocol: String?,
    val firmware: String?,
    val enabled: Boolean = true,
    val status: PrinterStatus? = null,
    val attributes: PrinterAttributes? = null
)

data class PrinterStatus(
    val status: String,
    val printing: Boolean = false,
    val filename: String? = null,
    @SerializedName("CurrentLayer")
    val currentLayer: Int? = null,
    @SerializedName("TotalLayer")
    val totalLayer: Int? = null,
    @SerializedName("PrintPercent")
    val printPercent: Int? = null,
    @SerializedName("PrintTimeRemaining")
    val printTimeRemaining: Int? = null
)

data class PrinterAttributes(
    @SerializedName("Temp")
    val temperature: Float? = null,
    @SerializedName("TempTarget")
    val targetTemperature: Float? = null,
    @SerializedName("MachineName")
    val machineName: String? = null,
    @SerializedName("FirmwareVersion")
    val firmwareVersion: String? = null,
    @SerializedName("Resolution")
    val resolution: String? = null,
    @SerializedName("MainboardIP")
    val mainboardIP: String? = null
)

data class PrintersResponse(
    val printers: Map<String, Printer>
)

data class PrintFile(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean = false
)

data class FilesResponse(
    val files: List<PrintFile>,
    val currentPath: String
)
