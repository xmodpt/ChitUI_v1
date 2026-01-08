/**
 * ChitUI Socket.IO Client for Real-time Updates
 *
 * This class handles WebSocket connections to ChitUI for real-time printer updates:
 * - Printer status changes
 * - Print progress updates
 * - File list changes
 * - Connection status
 *
 * Usage:
 *   val socketClient = ChitUISocketClient(context)
 *   socketClient.connect()
 *   socketClient.setOnPrinterUpdateListener { printers ->
 *       // Update UI with new printer data
 *   }
 */

package com.example.chitui.socket

import android.content.Context
import android.util.Log
import com.example.chitui.api.Printer
import com.example.chitui.api.TokenManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException

class ChitUISocketClient(private val context: Context) {

    private var socket: Socket? = null
    private val tokenManager = TokenManager(context)
    private val gson = Gson()

    // Change this to match your Raspberry Pi's IP address
    private val serverUrl = "http://192.168.1.100:8080"

    // Listeners for various events
    private var onConnectedListener: (() -> Unit)? = null
    private var onDisconnectedListener: (() -> Unit)? = null
    private var onPrinterUpdateListener: ((List<Printer>) -> Unit)? = null
    private var onPrinterInfoListener: ((Printer) -> Unit)? = null
    private var onErrorListener: ((String) -> Unit)? = null

    companion object {
        private const val TAG = "ChitUISocket"

        // Socket.IO events
        private const val EVENT_CONNECT = Socket.EVENT_CONNECT
        private const val EVENT_DISCONNECT = Socket.EVENT_DISCONNECT
        private const val EVENT_CONNECT_ERROR = Socket.EVENT_CONNECT_ERROR

        // ChitUI custom events
        private const val EVENT_PRINTERS = "printers"
        private const val EVENT_PRINTER_INFO = "printer_info"
        private const val EVENT_PRINTER_FILES = "printer_files"
        private const val EVENT_GET_ATTRIBUTES = "get_attributes"
    }

    /**
     * Connect to ChitUI Socket.IO server
     */
    fun connect() {
        try {
            // Create Socket.IO options
            val opts = IO.Options().apply {
                // Add JWT token for authentication (if your socket needs it)
                val token = tokenManager.getToken()
                if (token != null) {
                    auth = mapOf("token" to token)
                }

                // Connection options
                reconnection = true
                reconnectionDelay = 1000
                reconnectionAttempts = Int.MAX_VALUE
            }

            // Create socket connection
            socket = IO.socket(serverUrl, opts)

            // Set up event listeners
            setupSocketListeners()

            // Connect
            socket?.connect()
            Log.d(TAG, "Connecting to $serverUrl...")

        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid server URL: $serverUrl", e)
            onErrorListener?.invoke("Invalid server URL")
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to socket", e)
            onErrorListener?.invoke("Connection error: ${e.message}")
        }
    }

    /**
     * Disconnect from server
     */
    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        Log.d(TAG, "Disconnected from server")
    }

    /**
     * Check if socket is connected
     */
    fun isConnected(): Boolean {
        return socket?.connected() ?: false
    }

    /**
     * Request printer list update
     */
    fun requestPrinterUpdate() {
        if (isConnected()) {
            socket?.emit("get_printers")
            Log.d(TAG, "Requested printer update")
        } else {
            Log.w(TAG, "Cannot request update - not connected")
        }
    }

    /**
     * Request specific printer info
     *
     * @param printerId Printer ID to query
     */
    fun requestPrinterInfo(printerId: String) {
        if (isConnected()) {
            val json = JSONObject().apply {
                put("printer_id", printerId)
            }
            socket?.emit(EVENT_GET_ATTRIBUTES, json)
            Log.d(TAG, "Requested info for printer: $printerId")
        }
    }

    /**
     * Send print action command
     *
     * @param printerId Target printer ID
     * @param action Action type: "print", "pause", "resume", "stop"
     * @param fileName File name (required for "print" action)
     */
    fun sendPrintAction(printerId: String, action: String, fileName: String? = null) {
        if (isConnected()) {
            val json = JSONObject().apply {
                put("printer_id", printerId)
                put("action", action)
                if (fileName != null) {
                    put("file_name", fileName)
                }
            }
            socket?.emit("action_$action", json)
            Log.d(TAG, "Sent $action command to printer: $printerId")
        }
    }

    // ============ Event Listeners Setup ============

    private fun setupSocketListeners() {
        socket?.apply {
            // Connection events
            on(EVENT_CONNECT) {
                Log.d(TAG, "Connected to ChitUI server")
                onConnectedListener?.invoke()
            }

            on(EVENT_DISCONNECT) {
                Log.d(TAG, "Disconnected from ChitUI server")
                onDisconnectedListener?.invoke()
            }

            on(EVENT_CONNECT_ERROR) { args ->
                val error = args.firstOrNull()?.toString() ?: "Unknown error"
                Log.e(TAG, "Connection error: $error")
                onErrorListener?.invoke(error)
            }

            // Printer data events
            on(EVENT_PRINTERS) { args ->
                try {
                    val data = args.firstOrNull() as? JSONArray
                    if (data != null) {
                        val printerList = parsePrinterList(data)
                        onPrinterUpdateListener?.invoke(printerList)
                        Log.d(TAG, "Received ${printerList.size} printers")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing printers data", e)
                }
            }

            on(EVENT_PRINTER_INFO) { args ->
                try {
                    val data = args.firstOrNull() as? JSONObject
                    if (data != null) {
                        val printer = parsePrinter(data)
                        onPrinterInfoListener?.invoke(printer)
                        Log.d(TAG, "Received printer info: ${printer.id}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing printer info", e)
                }
            }
        }
    }

    // ============ Data Parsing ============

    private fun parsePrinterList(jsonArray: JSONArray): List<Printer> {
        val printers = mutableListOf<Printer>()
        for (i in 0 until jsonArray.length()) {
            try {
                val jsonObject = jsonArray.getJSONObject(i)
                printers.add(parsePrinter(jsonObject))
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing printer at index $i", e)
            }
        }
        return printers
    }

    private fun parsePrinter(jsonObject: JSONObject): Printer {
        val type = object : TypeToken<Printer>() {}.type
        return gson.fromJson(jsonObject.toString(), type)
    }

    // ============ Public Event Listener Setters ============

    fun setOnConnectedListener(listener: () -> Unit) {
        onConnectedListener = listener
    }

    fun setOnDisconnectedListener(listener: () -> Unit) {
        onDisconnectedListener = listener
    }

    fun setOnPrinterUpdateListener(listener: (List<Printer>) -> Unit) {
        onPrinterUpdateListener = listener
    }

    fun setOnPrinterInfoListener(listener: (Printer) -> Unit) {
        onPrinterInfoListener = listener
    }

    fun setOnErrorListener(listener: (String) -> Unit) {
        onErrorListener = listener
    }
}
