package com.chitui.client.data.remote

import com.chitui.client.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import timber.log.Timber
import java.net.URISyntaxException

class SocketIOManager(
    private val serverUrl: String,
    private val gson: Gson
) {
    private var socket: Socket? = null
    private val options = IO.Options().apply {
        transports = arrayOf("websocket")
        reconnection = true
        reconnectionDelay = 1000
        reconnectionDelayMax = 5000
        timeout = 10000
    }

    fun connect(): Flow<ConnectionState> = callbackFlow {
        try {
            socket = IO.socket(serverUrl, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Timber.d("Socket connected")
                trySend(ConnectionState.Connected)
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Timber.d("Socket disconnected")
                trySend(ConnectionState.Disconnected)
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Timber.e("Socket connection error: ${args.firstOrNull()}")
                trySend(ConnectionState.Error(args.firstOrNull()?.toString() ?: "Connection error"))
            }

            socket?.connect()
        } catch (e: URISyntaxException) {
            Timber.e(e, "Invalid server URL")
            trySend(ConnectionState.Error("Invalid server URL"))
        }

        awaitClose {
            socket?.disconnect()
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    // Printer Events
    fun requestPrinters() {
        socket?.emit("printers")
    }

    fun observePrinters(): Flow<List<Printer>> = callbackFlow {
        val listener = { args: Array<Any> ->
            try {
                val json = args.firstOrNull() as? JSONObject
                val printersJson = json?.optJSONArray("printers")?.toString()
                val type = object : TypeToken<List<Printer>>() {}.type
                val printers: List<Printer> = gson.fromJson(printersJson, type) ?: emptyList()
                trySend(printers)
            } catch (e: Exception) {
                Timber.e(e, "Error parsing printers")
            }
        }

        socket?.on("printers", listener)

        awaitClose {
            socket?.off("printers", listener)
        }
    }

    fun requestPrinterInfo(printerId: String) {
        socket?.emit("printer_info", JSONObject().apply {
            put("printer_id", printerId)
        })
    }

    fun observePrinterStatus(): Flow<Pair<String, PrinterStatus>> = callbackFlow {
        val listener = { args: Array<Any> ->
            try {
                val json = args.firstOrNull() as? JSONObject
                val printerId = json?.optString("printer_id") ?: return@callbackFlow
                val statusJson = json.optJSONObject("status")?.toString()
                val status: PrinterStatus? = gson.fromJson(statusJson, PrinterStatus::class.java)
                if (status != null) {
                    trySend(Pair(printerId, status))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error parsing printer status")
            }
        }

        socket?.on("printer_status", listener)

        awaitClose {
            socket?.off("printer_status", listener)
        }
    }

    fun observePrinterAttributes(): Flow<Pair<String, PrinterAttributes>> = callbackFlow {
        val listener = { args: Array<Any> ->
            try {
                val json = args.firstOrNull() as? JSONObject
                val printerId = json?.optString("printer_id") ?: return@callbackFlow
                val attrsJson = json.optJSONObject("attributes")?.toString()
                val attrs: PrinterAttributes? = gson.fromJson(attrsJson, PrinterAttributes::class.java)
                if (attrs != null) {
                    trySend(Pair(printerId, attrs))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error parsing printer attributes")
            }
        }

        socket?.on("printer_attributes", listener)

        awaitClose {
            socket?.off("printer_attributes", listener)
        }
    }

    // File Events
    fun requestFiles(printerId: String) {
        socket?.emit("printer_files", JSONObject().apply {
            put("printer_id", printerId)
        })
    }

    fun observeFileList(): Flow<Pair<String, FileListData>> = callbackFlow {
        val listener = { args: Array<Any> ->
            try {
                val json = args.firstOrNull() as? JSONObject
                val printerId = json?.optString("printer_id") ?: return@callbackFlow
                val dataJson = json.optJSONObject("data")?.toString()
                val data: FileListData? = gson.fromJson(dataJson, FileListData::class.java)
                if (data != null) {
                    trySend(Pair(printerId, data))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error parsing file list")
            }
        }

        socket?.on("file_list", listener)

        awaitClose {
            socket?.off("file_list", listener)
        }
    }

    // Print Actions
    fun deleteFile(printerId: String, fileUrl: String) {
        socket?.emit("action_delete", JSONObject().apply {
            put("printer_id", printerId)
            put("file_url", fileUrl)
        })
    }

    fun startPrint(printerId: String, fileUrl: String, startLayer: Int = 0) {
        socket?.emit("action_print", JSONObject().apply {
            put("printer_id", printerId)
            put("file_url", fileUrl)
            put("start_layer", startLayer)
        })
    }

    fun pausePrint(printerId: String) {
        socket?.emit("action_pause", JSONObject().apply {
            put("printer_id", printerId)
        })
    }

    fun resumePrint(printerId: String) {
        socket?.emit("action_resume", JSONObject().apply {
            put("printer_id", printerId)
        })
    }

    fun stopPrint(printerId: String) {
        socket?.emit("action_stop", JSONObject().apply {
            put("printer_id", printerId)
        })
    }

    fun clearHistory(printerId: String) {
        socket?.emit("action_clear_history", JSONObject().apply {
            put("printer_id", printerId)
        })
    }

    fun wipeStorage(printerId: String) {
        socket?.emit("action_wipe_storage", JSONObject().apply {
            put("printer_id", printerId)
        })
    }

    // Responses and Notifications
    fun observePrinterResponse(): Flow<Pair<String, JsonObject>> = callbackFlow {
        val listener = { args: Array<Any> ->
            try {
                val json = args.firstOrNull() as? JSONObject
                val printerId = json?.optString("printer_id") ?: return@callbackFlow
                val responseJson = json.optJSONObject("response")?.toString()
                val response: JsonObject? = gson.fromJson(responseJson, JsonObject::class.java)
                if (response != null) {
                    trySend(Pair(printerId, response))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error parsing printer response")
            }
        }

        socket?.on("printer_response", listener)

        awaitClose {
            socket?.off("printer_response", listener)
        }
    }

    fun observeToast(): Flow<ToastMessage> = callbackFlow {
        val listener = { args: Array<Any> ->
            try {
                val json = args.firstOrNull() as? JSONObject
                val message = json?.optString("message") ?: return@callbackFlow
                val type = json.optString("type", "info")
                trySend(ToastMessage(message, type))
            } catch (e: Exception) {
                Timber.e(e, "Error parsing toast")
            }
        }

        socket?.on("toast", listener)

        awaitClose {
            socket?.off("toast", listener)
        }
    }

    // Terminal Plugin
    fun sendTerminalCommand(printerId: String, command: String) {
        socket?.emit("terminal_command", JSONObject().apply {
            put("printer_id", printerId)
            put("command", command)
        })
    }

    fun observeTerminalMessages(): Flow<TerminalMessage> = callbackFlow {
        val listener = { args: Array<Any> ->
            try {
                val json = args.firstOrNull() as? JSONObject
                val msgJson = json?.toString()
                val msg: TerminalMessage? = gson.fromJson(msgJson, TerminalMessage::class.java)
                if (msg != null) {
                    trySend(msg)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error parsing terminal message")
            }
        }

        socket?.on("terminal_message", listener)

        awaitClose {
            socket?.off("terminal_message", listener)
        }
    }

    sealed class ConnectionState {
        object Connected : ConnectionState()
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    data class ToastMessage(
        val message: String,
        val type: String // info, success, error, warning
    )
}
