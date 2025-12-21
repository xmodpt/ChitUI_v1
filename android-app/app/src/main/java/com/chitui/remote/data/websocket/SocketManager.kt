package com.chitui.remote.data.websocket

import android.util.Log
import com.chitui.remote.data.models.Printer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class SocketManager {

    private var socket: Socket? = null
    private val gson = Gson()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _printers = MutableStateFlow<Map<String, Printer>>(emptyMap())
    val printers: StateFlow<Map<String, Printer>> = _printers.asStateFlow()

    private val _printerStatus = MutableStateFlow<PrinterStatusUpdate?>(null)
    val printerStatus: StateFlow<PrinterStatusUpdate?> = _printerStatus.asStateFlow()

    fun connect(serverUrl: String) {
        try {
            val options = IO.Options().apply {
                transports = arrayOf("websocket")
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
            }

            socket = IO.socket(serverUrl, options)

            socket?.apply {
                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "Socket connected")
                    _connectionState.value = ConnectionState.Connected
                    emit("printers", JSONObject())
                }

                on(Socket.EVENT_DISCONNECT) {
                    Log.d(TAG, "Socket disconnected")
                    _connectionState.value = ConnectionState.Disconnected
                }

                on(Socket.EVENT_CONNECT_ERROR) {
                    Log.e(TAG, "Socket connection error")
                    _connectionState.value = ConnectionState.Error("Connection error")
                }

                // Printer updates
                on("printers") { args ->
                    try {
                        val data = args[0] as JSONObject
                        val type = object : TypeToken<Map<String, Printer>>() {}.type
                        val printersMap: Map<String, Printer> = gson.fromJson(data.toString(), type)
                        _printers.value = printersMap
                        Log.d(TAG, "Received printers update: ${printersMap.size} printers")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing printers", e)
                    }
                }

                on("printer_status") { args ->
                    try {
                        val data = args[0] as JSONObject
                        val status = gson.fromJson(data.toString(), PrinterStatusUpdate::class.java)
                        _printerStatus.value = status
                        Log.d(TAG, "Received printer status: ${status.printerId}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing printer status", e)
                    }
                }

                on("printer_attributes") { args ->
                    try {
                        val data = args[0] as JSONObject
                        Log.d(TAG, "Received printer attributes: $data")
                        // You can add specific handling for attributes if needed
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing printer attributes", e)
                    }
                }

                on("printer_error") { args ->
                    try {
                        val data = args[0] as JSONObject
                        Log.e(TAG, "Printer error: $data")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing printer error", e)
                    }
                }

                connect()
                _connectionState.value = ConnectionState.Connecting
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error creating socket", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun requestPrinters() {
        socket?.emit("printers", JSONObject())
    }

    fun isConnected(): Boolean {
        return socket?.connected() == true
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    data class PrinterStatusUpdate(
        val printerId: String,
        val status: String,
        val printing: Boolean,
        val currentLayer: Int?,
        val totalLayer: Int?,
        val printPercent: Int?,
        val printTimeRemaining: Int?
    )

    companion object {
        private const val TAG = "SocketManager"

        @Volatile
        private var instance: SocketManager? = null

        fun getInstance(): SocketManager {
            return instance ?: synchronized(this) {
                instance ?: SocketManager().also { instance = it }
            }
        }
    }
}
