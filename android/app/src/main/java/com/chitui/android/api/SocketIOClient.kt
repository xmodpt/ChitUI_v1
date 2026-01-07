package com.chitui.android.api

import android.util.Log
import com.chitui.android.data.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

class SocketIOClient {

    private var socket: Socket? = null
    private val gson = Gson()

    private val _events = MutableSharedFlow<SocketEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

    private val _connectionState = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 1)
    val connectionState: SharedFlow<Boolean> = _connectionState.asSharedFlow()

    fun connect(serverUrl: String) {
        try {
            val uri = URI.create(serverUrl.trimEnd('/'))

            val options = IO.Options().apply {
                transports = arrayOf("websocket", "polling")
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 20000
            }

            socket = IO.socket(uri, options)

            socket?.apply {
                // Connection events
                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "SocketIO Connected")
                    _connectionState.tryEmit(true)
                    _events.tryEmit(SocketEvent.Connected)
                }

                on(Socket.EVENT_DISCONNECT) {
                    Log.d(TAG, "SocketIO Disconnected")
                    _connectionState.tryEmit(false)
                    _events.tryEmit(SocketEvent.Disconnected)
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    Log.e(TAG, "SocketIO Connection Error: ${args.firstOrNull()}")
                    _connectionState.tryEmit(false)
                }

                // ChitUI specific events
                on("printers") { args ->
                    try {
                        val data = args.firstOrNull() as? JSONArray
                        if (data != null) {
                            val printers = parsePrinters(data)
                            _events.tryEmit(SocketEvent.PrintersUpdate(printers))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing printers: ${e.message}")
                    }
                }

                on("printer_status") { args ->
                    try {
                        val data = args.firstOrNull() as? JSONObject
                        if (data != null) {
                            val printerId = data.optString("printer_id")
                            val status = parsePrinterStatus(data.optJSONObject("status"))
                            _events.tryEmit(SocketEvent.PrinterStatus(printerId, status))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing printer status: ${e.message}")
                    }
                }

                on("printer_error") { args ->
                    try {
                        val data = args.firstOrNull() as? JSONObject
                        if (data != null) {
                            val printerId = data.optString("printer_id")
                            val error = data.optString("error")
                            _events.tryEmit(SocketEvent.PrinterError(printerId, error))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing printer error: ${e.message}")
                    }
                }

                on("printer_notice") { args ->
                    try {
                        val data = args.firstOrNull() as? JSONObject
                        if (data != null) {
                            val printerId = data.optString("printer_id")
                            val notice = data.optString("notice")
                            _events.tryEmit(SocketEvent.PrinterNotice(printerId, notice))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing printer notice: ${e.message}")
                    }
                }

                on("upload_progress") { args ->
                    try {
                        val data = args.firstOrNull() as? JSONObject
                        if (data != null) {
                            val filename = data.optString("filename")
                            val progress = data.optInt("progress")
                            _events.tryEmit(SocketEvent.UploadProgress(filename, progress))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing upload progress: ${e.message}")
                    }
                }

                on("toast") { args ->
                    try {
                        val data = args.firstOrNull() as? JSONObject
                        if (data != null) {
                            val message = data.optString("message")
                            val type = data.optString("type", "info")
                            _events.tryEmit(SocketEvent.Toast(message, type))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing toast: ${e.message}")
                    }
                }

                on("refresh_page") {
                    _events.tryEmit(SocketEvent.RefreshPage)
                }

                connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating socket: ${e.message}")
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }

    fun isConnected(): Boolean = socket?.connected() ?: false

    // Emit events to server
    fun requestPrinters() {
        socket?.emit("printers")
    }

    fun requestPrinterInfo(printerId: String) {
        socket?.emit("printer_info", JSONObject().put("printer_id", printerId))
    }

    fun requestPrinterFiles(printerId: String) {
        socket?.emit("printer_files", JSONObject().put("printer_id", printerId))
    }

    fun startPrint(printerId: String, filename: String, startLayer: Int = 0) {
        val data = JSONObject().apply {
            put("printer_id", printerId)
            put("filename", filename)
            put("start_layer", startLayer)
        }
        socket?.emit("action_print", data)
    }

    fun pausePrint(printerId: String) {
        socket?.emit("action_pause", JSONObject().put("printer_id", printerId))
    }

    fun resumePrint(printerId: String) {
        socket?.emit("action_resume", JSONObject().put("printer_id", printerId))
    }

    fun stopPrint(printerId: String) {
        socket?.emit("action_stop", JSONObject().put("printer_id", printerId))
    }

    fun deleteFile(printerId: String, filename: String) {
        val data = JSONObject().apply {
            put("printer_id", printerId)
            put("filename", filename)
        }
        socket?.emit("action_delete", data)
    }

    fun clearHistory(printerId: String) {
        socket?.emit("action_clear_history", JSONObject().put("printer_id", printerId))
    }

    fun wipeStorage(printerId: String) {
        socket?.emit("action_wipe_storage", JSONObject().put("printer_id", printerId))
    }

    fun getAttributes(printerId: String) {
        socket?.emit("get_attributes", JSONObject().put("printer_id", printerId))
    }

    fun getTaskDetails(printerId: String) {
        socket?.emit("get_task_details", JSONObject().put("printer_id", printerId))
    }

    // Helper methods
    private fun parsePrinters(jsonArray: JSONArray): List<Printer> {
        val printers = mutableListOf<Printer>()
        for (i in 0 until jsonArray.length()) {
            try {
                val obj = jsonArray.getJSONObject(i)
                val printer = Printer(
                    id = obj.optString("id", ""),
                    name = obj.optString("name", "Unknown"),
                    ip = obj.optString("ip", ""),
                    port = obj.optInt("port", 3030),
                    model = obj.optString("model"),
                    machineName = obj.optString("machine_name"),
                    mainboardId = obj.optString("mainboard_id"),
                    firmwareVersion = obj.optString("firmware_version"),
                    isDefault = obj.optBoolean("is_default", false),
                    isConnected = obj.optBoolean("connected", false),
                    status = obj.optJSONObject("status")?.let { parsePrinterStatus(it) }
                )
                printers.add(printer)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing printer at index $i: ${e.message}")
            }
        }
        return printers
    }

    private fun parsePrinterStatus(json: JSONObject?): PrinterStatus {
        if (json == null) return PrinterStatus()

        val stateStr = json.optString("state", "idle").lowercase()
        val state = when (stateStr) {
            "printing" -> PrinterState.PRINTING
            "paused" -> PrinterState.PAUSED
            "error" -> PrinterState.ERROR
            "offline" -> PrinterState.OFFLINE
            "idle" -> PrinterState.IDLE
            else -> PrinterState.UNKNOWN
        }

        return PrinterStatus(
            state = state,
            currentFile = json.optString("current_file"),
            progress = json.optInt("progress", 0),
            currentLayer = json.optInt("current_layer", 0),
            totalLayers = json.optInt("total_layers", 0),
            printTimeRemaining = json.optInt("time_remaining", 0),
            printTimeElapsed = json.optInt("time_elapsed", 0),
            errorMessage = json.optString("error")
        )
    }

    companion object {
        private const val TAG = "SocketIOClient"
    }
}
