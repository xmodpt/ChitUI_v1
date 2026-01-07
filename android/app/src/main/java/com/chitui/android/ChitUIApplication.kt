package com.chitui.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.chitui.android.data.ChitUIRepository
import com.chitui.android.data.PreferencesManager

class ChitUIApplication : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var repository: ChitUIRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize preferences and repository
        preferencesManager = PreferencesManager(this)
        repository = ChitUIRepository(preferencesManager)

        // Create notification channel
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Printer Status",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about printer status and print jobs"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "chitui_printer_status"
    }
}
