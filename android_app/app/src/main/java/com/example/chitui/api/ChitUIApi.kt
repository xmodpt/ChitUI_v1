/**
 * ChitUI Mobile API Interface
 *
 * Retrofit interface defining all ChitUI API endpoints
 * Use this interface to communicate with your Raspberry Pi running ChitUI
 *
 * Setup in your Android app:
 * 1. Add dependencies to build.gradle
 * 2. Create Retrofit instance (see RetrofitClient.kt)
 * 3. Call API methods using this interface
 */

package com.example.chitui.api

import retrofit2.Response
import retrofit2.http.*

// ============ Data Models ============

data class LoginRequest(
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val token: String?,
    val expires_in: Long?,
    val user_id: String?,
    val require_password_change: Boolean?,
    val message: String?
)

data class RefreshTokenResponse(
    val success: Boolean,
    val token: String?,
    val expires_in: Long?,
    val message: String?
)

data class PrintersResponse(
    val success: Boolean,
    val printers: List<Printer>?,
    val count: Int?,
    val message: String?
)

data class Printer(
    val id: String,
    val name: String?,
    val ip: String?,
    val status: String?,
    val current_file: String?,
    val progress: Int?,
    val MainboardID: String?,
    val Attributes: PrinterAttributes?
)

data class PrinterAttributes(
    val MachineName: String?,
    val MachineType: String?,
    val CurrentStatus: String?,
    val CurrentFile: String?,
    val FileList: List<PrintFile>?
)

data class PrintFile(
    val FileName: String?,
    val FileSize: Long?,
    val CreatTime: String?
)

data class PrinterInfoResponse(
    val success: Boolean,
    val printer: Printer?,
    val message: String?
)

data class StatusResponse(
    val success: Boolean,
    val status: SystemStatus?,
    val message: String?
)

data class SystemStatus(
    val usb_gadget_enabled: Boolean,
    val usb_gadget_available: Boolean,
    val usb_auto_refresh: Boolean,
    val camera_support: Boolean,
    val printer_count: Int,
    val active_connections: Int
)

// ============ API Interface ============

interface ChitUIApi {

    /**
     * Login and get JWT token
     *
     * @param loginRequest Contains password
     * @return LoginResponse with JWT token
     */
    @POST("/api/mobile/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    /**
     * Refresh JWT token
     * Requires Authorization header with current token
     *
     * @return RefreshTokenResponse with new token
     */
    @POST("/api/mobile/refresh-token")
    suspend fun refreshToken(): Response<RefreshTokenResponse>

    /**
     * Get list of all printers
     * Requires Authorization header
     *
     * @return PrintersResponse with list of printers
     */
    @GET("/api/mobile/printers")
    suspend fun getPrinters(): Response<PrintersResponse>

    /**
     * Get detailed info about specific printer
     * Requires Authorization header
     *
     * @param printerId Printer ID
     * @return PrinterInfoResponse with printer details
     */
    @GET("/api/mobile/printer/{printer_id}/info")
    suspend fun getPrinterInfo(@Path("printer_id") printerId: String): Response<PrinterInfoResponse>

    /**
     * Get ChitUI system status
     * Requires Authorization header
     *
     * @return StatusResponse with system information
     */
    @GET("/api/mobile/status")
    suspend fun getStatus(): Response<StatusResponse>

    // ============ Additional Endpoints (using existing routes) ============

    /**
     * Discover printers on network
     * Requires Authorization header
     */
    @POST("/discover")
    suspend fun discoverPrinters(): Response<Any>

    /**
     * Upload G-code file
     * Requires Authorization header
     *
     * @param printerId Target printer ID
     * @param file Multipart file data
     */
    @Multipart
    @POST("/upload")
    suspend fun uploadFile(
        @Query("printer_id") printerId: String,
        @Part file: okhttp3.MultipartBody.Part
    ): Response<Any>
}
