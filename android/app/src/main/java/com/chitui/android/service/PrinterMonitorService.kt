package com.chitui.android.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chitui.android.ChitUIApplication
import com.chitui.android.MainActivity
import com.chitui.android.R
import com.chitui.android.data.PrinterState
import com.chitui.android.data.SocketEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class PrinterMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var notificationManager: NotificationManager
    private val app: ChitUIApplication
        get() = application as ChitUIApplication

    private var notifyPrintComplete = true
    private var notifyErrors = true

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Load notification preferences
        serviceScope.launch {
            notifyPrintComplete = app.preferencesManager.getNotifyPrintComplete().first()
            notifyErrors = app.preferencesManager.getNotifyErrors().first()
        }

        // Start foreground service
        startForeground(NOTIFICATION_ID, createForegroundNotification())

        // Observe socket events
        serviceScope.launch {
            app.repository.socketEvents.collect { event ->
                handleSocketEvent(event)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handleSocketEvent(event: SocketEvent) {
        when (event) {
            is SocketEvent.PrinterStatus -> {
                val status = event.status
                val printerId = event.printerId

                // Notify on print completion
                if (notifyPrintComplete && status.state == PrinterState.IDLE && status.progress == 100) {
                    showNotification(
                        title = "Print Complete",
                        message = "Print job finished on printer $printerId",
                        notificationId = printerId.hashCode()
                    )
                }
            }

            is SocketEvent.PrinterError -> {
                if (notifyErrors) {
                    showNotification(
                        title = "Printer Error",
                        message = event.error,
                        notificationId = event.printerId.hashCode(),
                        priority = NotificationCompat.PRIORITY_HIGH
                    )
                }
            }

            else -> {}
        }
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, ChitUIApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("ChitUI")
            .setContentText("Monitoring printer status")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showNotification(
        title: String,
        message: String,
        notificationId: Int,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, ChitUIApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(priority)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, PrinterMonitorService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PrinterMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
