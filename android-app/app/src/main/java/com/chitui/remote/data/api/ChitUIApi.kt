package com.chitui.remote.data.api

import com.chitui.remote.data.models.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ChitUIApi {

    // Authentication
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse>

    // Settings
    @GET("settings")
    suspend fun getSettings(): Response<Settings>

    @POST("settings")
    suspend fun updateSettings(@Body settings: Settings): Response<ApiResponse>

    // Printers
    @GET("printers")
    suspend fun getPrinters(): Response<PrintersResponse>

    @POST("discover")
    suspend fun discoverPrinters(): Response<ApiResponse>

    @POST("printer/manual")
    suspend fun addManualPrinter(@Body printer: PrinterConfig): Response<ApiResponse>

    @DELETE("printer/{printerId}")
    suspend fun removePrinter(@Path("printerId") printerId: String): Response<ApiResponse>

    @POST("printer/default")
    suspend fun setDefaultPrinter(@Body request: Map<String, String>): Response<ApiResponse>

    // Print Operations
    @POST("printer/{printerId}/start")
    suspend fun startPrint(
        @Path("printerId") printerId: String,
        @Body file: Map<String, String>
    ): Response<ApiResponse>

    @POST("printer/{printerId}/pause")
    suspend fun pausePrint(@Path("printerId") printerId: String): Response<ApiResponse>

    @POST("printer/{printerId}/resume")
    suspend fun resumePrint(@Path("printerId") printerId: String): Response<ApiResponse>

    @POST("printer/{printerId}/stop")
    suspend fun stopPrint(@Path("printerId") printerId: String): Response<ApiResponse>

    // Files
    @GET("files")
    suspend fun getFiles(@Query("path") path: String? = null): Response<FilesResponse>

    @Multipart
    @POST("upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Query("printer_id") printerId: String? = null
    ): Response<ApiResponse>

    @DELETE("file")
    suspend fun deleteFile(
        @Query("path") path: String,
        @Query("printer_id") printerId: String
    ): Response<ApiResponse>

    // System
    @GET("system/info")
    suspend fun getSystemInfo(): Response<SystemInfo>

    @POST("maintenance/restart")
    suspend fun restartApp(): Response<ApiResponse>

    @POST("maintenance/reboot")
    suspend fun rebootSystem(): Response<ApiResponse>

    // USB Gadget
    @GET("usb-gadget/storage")
    suspend fun getStorageInfo(): Response<Map<String, Any>>

    @POST("usb-gadget/refresh")
    suspend fun refreshUSB(): Response<ApiResponse>

    // Camera
    @GET("camera/{printerId}/snapshot")
    suspend fun getCameraSnapshot(@Path("printerId") printerId: String): Response<ResponseBody>
}
