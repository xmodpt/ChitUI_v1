package com.chitui.client.data.remote

import com.chitui.client.data.model.*
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ChitUIApiService {

    // Authentication
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<JsonObject>

    @GET("auth/session-timeout")
    suspend fun getSessionTimeout(): Response<Int>

    @POST("auth/session-timeout")
    suspend fun setSessionTimeout(@Body timeout: Int): Response<Unit>

    // Printer Management
    @POST("discover")
    suspend fun discoverPrinters(): Response<Unit>

    @POST("printer/manual")
    suspend fun addPrinterManual(@Body printer: JsonObject): Response<Unit>

    @PUT("printer/{printer_id}")
    suspend fun updatePrinter(
        @Path("printer_id") printerId: String,
        @Body printer: JsonObject
    ): Response<Unit>

    @DELETE("printer/{printer_id}")
    suspend fun deletePrinter(@Path("printer_id") printerId: String): Response<Unit>

    @POST("printer/default")
    suspend fun setDefaultPrinter(@Body printerId: JsonObject): Response<Unit>

    @GET("printer/images")
    suspend fun getPrinterImages(): Response<List<String>>

    // File Operations
    @Multipart
    @POST("upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("printer_id") printerId: RequestBody,
        @Part("destination") destination: RequestBody
    ): Response<JsonObject>

    @GET("thumbnail/{printer_id}")
    suspend fun getThumbnail(@Path("printer_id") printerId: String): Response<ResponseBody>

    // Storage
    @GET("usb-gadget/storage")
    suspend fun getUSBGadgetStorage(): Response<USBGadgetStatus>

    @POST("usb-gadget/refresh")
    suspend fun refreshUSBGadget(): Response<Unit>

    // System
    @GET("settings")
    suspend fun getSettings(): Response<ServerSettings>

    @POST("settings")
    suspend fun updateSettings(@Body settings: ServerSettings): Response<Unit>

    @GET("status")
    suspend fun getStatus(): Response<JsonObject>

    @GET("python-packages")
    suspend fun getPythonPackages(): Response<List<String>>

    @POST("maintenance/restart")
    suspend fun restartServer(): Response<Unit>

    @POST("maintenance/reboot")
    suspend fun rebootSystem(): Response<Unit>

    // Plugins
    @GET("plugins")
    suspend fun getPlugins(): Response<PluginListResponse>

    @POST("plugins/{plugin_id}/enable")
    suspend fun enablePlugin(@Path("plugin_id") pluginId: String): Response<Unit>

    @POST("plugins/{plugin_id}/disable")
    suspend fun disablePlugin(@Path("plugin_id") pluginId: String): Response<Unit>

    @DELETE("plugins/{plugin_id}")
    suspend fun deletePlugin(@Path("plugin_id") pluginId: String): Response<Unit>

    @Multipart
    @POST("plugins/upload")
    suspend fun uploadPlugin(@Part file: MultipartBody.Part): Response<JsonObject>

    @GET("plugins/ui")
    suspend fun getPluginUI(): Response<JsonObject>

    // Camera
    @POST("camera/start")
    suspend fun startCamera(@Body printerId: JsonObject): Response<Unit>

    @POST("camera/stop")
    suspend fun stopCamera(@Body printerId: JsonObject): Response<Unit>

    @GET("camera/video")
    suspend fun getCameraStream(): Response<ResponseBody>

    // GPIO Relay Plugin
    @GET("plugin/gpio_relay_control/status")
    suspend fun getRelayStatus(): Response<RelayState>

    @POST("plugin/gpio_relay_control/relay/{num}/toggle")
    suspend fun toggleRelay(@Path("num") relayNum: Int): Response<JsonObject>

    @POST("plugin/gpio_relay_control/relay/{num}/set")
    suspend fun setRelay(
        @Path("num") relayNum: Int,
        @Body state: JsonObject
    ): Response<JsonObject>

    @GET("plugin/gpio_relay_control/config")
    suspend fun getRelayConfig(): Response<RelayConfig>

    @POST("plugin/gpio_relay_control/config")
    suspend fun setRelayConfig(@Body config: RelayConfig): Response<Unit>

    // IP Camera Plugin
    @GET("plugin/ip_camera/cameras")
    suspend fun getIPCameras(): Response<IPCameraListResponse>

    @POST("plugin/ip_camera/camera/{id}/start")
    suspend fun startIPCamera(@Path("id") cameraId: String): Response<Unit>

    @POST("plugin/ip_camera/camera/{id}/stop")
    suspend fun stopIPCamera(@Path("id") cameraId: String): Response<Unit>

    @GET("plugin/ip_camera/camera/{id}/video")
    suspend fun getIPCameraStream(@Path("id") cameraId: String): Response<ResponseBody>

    @GET("plugin/ip_camera/config")
    suspend fun getIPCameraConfig(): Response<JsonObject>

    @POST("plugin/ip_camera/config")
    suspend fun setIPCameraConfig(@Body config: JsonObject): Response<Unit>

    // RPi Stats Plugin
    @GET("plugin/rpi_stats/system-info")
    suspend fun getSystemInfo(): Response<SystemInfo>

    @GET("plugin/rpi_stats/stats")
    suspend fun getSystemStats(): Response<SystemStats>
}
