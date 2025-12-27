package com.chitui.client.util

import android.content.Context
import com.chitui.client.data.remote.ChitUIApiService
import com.chitui.client.data.remote.SocketIOManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

object NetworkModule {

    private var retrofit: Retrofit? = null
    private var socketIOManager: SocketIOManager? = null
    private lateinit var preferencesManager: PreferencesManager
    private val cookieJar = ChitUICookieJar()

    fun initialize(context: Context) {
        preferencesManager = PreferencesManager(context)
    }

    fun createApiService(baseUrl: String): ChitUIApiService {
        if (retrofit?.baseUrl().toString() != baseUrl || retrofit == null) {
            retrofit = createRetrofit(baseUrl)
        }
        return retrofit!!.create(ChitUIApiService::class.java)
    }

    fun createSocketIOManager(baseUrl: String): SocketIOManager {
        if (socketIOManager == null) {
            val gson = GsonBuilder()
                .setLenient()
                .create()
            socketIOManager = SocketIOManager(baseUrl, gson)
        }
        return socketIOManager!!
    }

    fun resetSocketIOManager() {
        socketIOManager?.disconnect()
        socketIOManager = null
    }

    private fun createRetrofit(baseUrl: String): Retrofit {
        val gson = GsonBuilder()
            .setLenient()
            .create()

        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)

                // Save session cookies
                val cookies = cookieJar.loadForRequest(request.url)
                val sessionCookie = cookies.find { it.name == "session" }
                if (sessionCookie != null) {
                    runBlocking {
                        preferencesManager.saveSessionCookie(sessionCookie.value)
                    }
                }

                response
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private class ChitUICookieJar : CookieJar {
        private val cookieStore = mutableMapOf<String, List<Cookie>>()

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        fun clear() {
            cookieStore.clear()
        }
    }
}
