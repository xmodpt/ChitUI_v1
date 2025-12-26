package com.chitui.client.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Printer(
    @SerializedName("Id")
    val id: String,

    @SerializedName("Name")
    val name: String,

    @SerializedName("MachineName")
    val machineName: String,

    @SerializedName("BrandName")
    val brandName: String,

    @SerializedName("MainboardIP")
    val ip: String,

    @SerializedName("ProtocolVersion")
    val protocolVersion: String,

    @SerializedName("FirmwareVersion")
    val firmwareVersion: String,

    @SerializedName("MainboardID")
    val mainboardId: String,

    val online: Boolean = false,
    val enabled: Boolean = true,
    val usbDeviceType: String? = null
) : Parcelable

@Parcelize
data class PrinterStatus(
    val machineStatus: Int,
    val printStatus: Int,
    val currentLayer: Int,
    val totalLayer: Int,
    val currentTicks: Long,
    val totalTicks: Long,
    val progress: Float,
    val currentFileName: String?,
    val errorCode: Int?
) : Parcelable

@Parcelize
data class PrinterAttributes(
    val machineName: String?,
    val brandName: String?,
    val size: PrintSize?,
    val protocolVersion: String?,
    val firmwareVersion: String?,
    val mainboardId: String?,
    val dataTransferUnit: Int?,
    val maxDataTransferUnit: Int?
) : Parcelable

@Parcelize
data class PrintSize(
    val x: Float,
    val y: Float,
    val z: Float
) : Parcelable

@Parcelize
data class PrinterTemperature(
    val currentTemp: Float,
    val targetTemp: Float,
    val maxTemp: Float
) : Parcelable

data class PrinterListResponse(
    val printers: List<Printer>
)
