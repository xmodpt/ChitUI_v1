/**
 * Extended ChitUI API with additional data models
 * This extends the basic API with printer-specific fields
 */

package com.example.chitui.api

// Extended Printer model with image URL
data class PrinterExtended(
    val id: String,
    val name: String?,
    val ip: String?,
    val port: Int?,
    val status: String?,
    val current_file: String?,
    val progress: Int?,
    val MainboardID: String?,
    val Attributes: PrinterAttributesExtended?,

    // Image URL constructed from server
    val image_url: String? = null
) {
    companion object {
        fun fromPrinter(printer: Printer, serverUrl: String): PrinterExtended {
            return PrinterExtended(
                id = printer.id,
                name = printer.name,
                ip = printer.ip,
                port = 3030,
                status = printer.status,
                current_file = printer.current_file,
                progress = printer.progress,
                MainboardID = printer.MainboardID,
                Attributes = printer.Attributes?.let {
                    PrinterAttributesExtended(
                        MachineName = it.MachineName,
                        MachineType = it.MachineType,
                        CurrentStatus = it.CurrentStatus,
                        CurrentFile = it.CurrentFile,
                        FileList = it.FileList,
                        PrintingLayer = parseIntSafe(it.PrintingLayer),
                        TotalLayer = parseIntSafe(it.TotalLayer),
                        PrintedTime = parseIntSafe(it.PrintedTime),
                        RemainTime = parseIntSafe(it.RemainTime),
                        Temperature = parseIntSafe(it.Temperature)
                    )
                },
                // Construct image URL
                image_url = "$serverUrl/printer/images?id=${printer.id}"
            )
        }

        private fun parseIntSafe(value: Any?): Int? {
            return when (value) {
                is Int -> value
                is String -> value.toIntOrNull()
                else -> null
            }
        }
    }
}

data class PrinterAttributesExtended(
    val MachineName: String?,
    val MachineType: String?,
    val CurrentStatus: String?,
    val CurrentFile: String?,
    val FileList: List<PrintFile>?,
    val PrintingLayer: Int?,
    val TotalLayer: Int?,
    val PrintedTime: Int?,
    val RemainTime: Int?,
    val Temperature: Int?
)
