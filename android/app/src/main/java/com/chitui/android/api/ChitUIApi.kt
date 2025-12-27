package com.chitui.android.api

import com.chitui.android.data.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ChitUIApi {

    // Authentication
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<ResponseBody>

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<Unit>>

    @GET("auth/session-timeout")
    suspend fun getSessionTimeout(): Response<ApiResponse<Int>>

    @POST("auth/session-timeout")
    suspend fun setSessionTimeout(@Body timeout: Map<String, Int>): Response<ApiResponse<Unit>>

    // Settings
    @GET("settings")
    suspend fun getSettings(): Response<Settings>

    @POST("settings")
    suspend fun updateSettings(@Body settings: SettingsUpdateRequest): Response<ApiResponse<Unit>>

    // Status
    @GET("status")
    suspend fun getStatus(): Response<AppStatus>

    // Printer Management
    @POST("discover")
    suspend fun discoverPrinters(@Body request: DiscoverRequest = DiscoverRequest()): Response<ApiResponse<Unit>>

    @POST("printer/manual")
    suspend fun addPrinterManually(@Body request: ManualPrinterRequest): Response<ApiResponse<Unit>>

    @PUT("printer/{printer_id}")
    suspend fun updatePrinter(
        @Path("printer_id") printerId: String,
        @Body printer: Map<String, Any>
    ): Response<ApiResponse<Unit>>

    @DELETE("printer/{printer_id}")
    suspend fun removePrinter(@Path("printer_id") printerId: String): Response<ApiResponse<Unit>>

    @POST("printer/default")
    suspend fun setDefaultPrinter(@Body printerId: Map<String, String>): Response<ApiResponse<Unit>>

    // File Upload
    @Multipart
    @POST("upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("printer_id") printerId: String? = null
    ): Response<UploadResponse>

    // Maintenance
    @POST("maintenance/restart")
    suspend fun restartApplication(): Response<ApiResponse<Unit>>

    @POST("maintenance/reboot")
    suspend fun rebootSystem(): Response<ApiResponse<Unit>>

    // Camera (if plugin enabled)
    @POST("camera/start")
    suspend fun startCamera(): Response<ApiResponse<Unit>>

    @POST("camera/stop")
    suspend fun stopCamera(): Response<ApiResponse<Unit>>

    @GET("camera/video")
    @Streaming
    suspend fun getCameraStream(): Response<ResponseBody>

    // GPIO Relay Plugin (if enabled)
    @GET("plugin/gpio_relay_control/status")
    suspend fun getRelayStatus(): Response<ApiResponse<Map<String, Boolean>>>

    @POST("plugin/gpio_relay_control/relay/{num}/toggle")
    suspend fun toggleRelay(@Path("num") relayNum: Int): Response<ApiResponse<Unit>>
}
