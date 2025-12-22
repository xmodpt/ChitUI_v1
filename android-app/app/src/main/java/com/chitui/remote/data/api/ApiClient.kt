package com.chitui.remote.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var retrofit: Retrofit? = null
    private var baseUrl: String = ""

    fun initialize(serverUrl: String) {
        baseUrl = serverUrl.trimEnd('/')

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cookieJar(PersistentCookieJar())
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl("$baseUrl/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getApi(): ChitUIApi {
        if (retrofit == null) {
            throw IllegalStateException("ApiClient not initialized. Call initialize() first.")
        }
        return retrofit!!.create(ChitUIApi::class.java)
    }

    fun getBaseUrl(): String = baseUrl

    fun isInitialized(): Boolean = retrofit != null
}
