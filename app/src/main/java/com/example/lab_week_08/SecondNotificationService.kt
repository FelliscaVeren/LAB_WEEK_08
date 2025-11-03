package com.example.lab_week_08

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData

class SecondNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "SecondNotificationChannel"
        const val NOTIFICATION_ID = 2001
        const val EXTRA_ID = "extra_id"

        val trackingCompletion = MutableLiveData<Boolean>()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(EXTRA_ID) ?: "Unknown"

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Third process done")
            .setContentText("Starting Second Notification Service for ID: $id")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Simulasi delay 4 detik agar tidak tabrakan dengan notifikasi sebelumnya
        Handler(mainLooper).postDelayed({
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val doneNotification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("All processes completed")
                .setContentText("Second Notification Service finished for ID: $id")
                .setSmallIcon(android.R.drawable.checkbox_on_background)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            manager.notify(NOTIFICATION_ID + 1, doneNotification)
            trackingCompletion.postValue(true)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }, 4000L)

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Second Notification Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
