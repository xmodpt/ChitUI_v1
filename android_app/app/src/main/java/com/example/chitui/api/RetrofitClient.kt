/**
 * Retrofit Client Configuration for ChitUI
 *
 * This singleton provides a configured Retrofit instance with:
 * - Automatic JWT token injection
 * - JSON serialization
 * - Logging (for debugging)
 * - Error handling
 *
 * Usage:
 *   val api = RetrofitClient.getInstance(context)
 *   val response = api.getPrinters()
 */

package com.example.chitui.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Change this to your Raspberry Pi's IP address or domain
    // Examples:
    // - Local network: "http://192.168.1.100:8080"
    // - Tailscale VPN: "http://100.64.0.1:8080"
    // - Public with HTTPS: "https://your-domain.com"
    private const val BASE_URL = "http://192.168.1.100:8080"

    private var api: ChitUIApi? = null
    private var tokenManager: TokenManager? = null

    /**
     * Get or create Retrofit API instance
     *
     * @param context Application context
     * @return Configured ChitUIApi instance
     */
    fun getInstance(context: Context): ChitUIApi {
        if (api == null) {
            tokenManager = TokenManager(context)
            api = createRetrofit()
        }
        return api!!
    }

    /**
     * Update base URL (useful for settings/configuration)
     *
     * @param newBaseUrl New server URL
     * @param context Application context
     */
    fun updateBaseUrl(newBaseUrl: String, context: Context) {
        tokenManager = TokenManager(context)
        api = createRetrofit(newBaseUrl)
    }

    private fun createRetrofit(baseUrl: String = BASE_URL): ChitUIApi {
        // Logging interceptor for debugging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Auth interceptor to automatically add JWT token to requests
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()

            // Skip adding token for login endpoint
            if (originalRequest.url.encodedPath.contains("/api/mobile/login")) {
                return@Interceptor chain.proceed(originalRequest)
            }

            // Add Authorization header with token
            val token = tokenManager?.getToken()
            val newRequest = if (token != null) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }

            chain.proceed(newRequest)
        }

        // Configure OkHttp client
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // Build Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ChitUIApi::class.java)
    }

    /**
     * Clear cached instance (useful for logout)
     */
    fun clearInstance() {
        api = null
        tokenManager?.clearToken()
    }
}


/**
 * Token Manager
 *
 * Handles JWT token storage and retrieval using SharedPreferences
 */
class TokenManager(context: Context) {

    private val prefs = context.getSharedPreferences("chitui_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_EXPIRY = "token_expiry"
    }

    /**
     * Save JWT token and expiry time
     *
     * @param token JWT token string
     * @param expiresIn Token validity in seconds
     */
    fun saveToken(token: String, expiresIn: Long) {
        val expiryTime = System.currentTimeMillis() + (expiresIn * 1000)
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_EXPIRY, expiryTime)
            .apply()
    }

    /**
     * Get stored JWT token (if not expired)
     *
     * @return Token string or null if expired/not found
     */
    fun getToken(): String? {
        val token = prefs.getString(KEY_TOKEN, null)
        val expiryTime = prefs.getLong(KEY_EXPIRY, 0)

        // Check if token is expired
        if (token != null && System.currentTimeMillis() < expiryTime) {
            return token
        }

        // Token expired or doesn't exist
        clearToken()
        return null
    }

    /**
     * Check if user is logged in (has valid token)
     *
     * @return true if logged in with valid token
     */
    fun isLoggedIn(): Boolean {
        return getToken() != null
    }

    /**
     * Clear stored token (logout)
     */
    fun clearToken() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EXPIRY)
            .apply()
    }
}
