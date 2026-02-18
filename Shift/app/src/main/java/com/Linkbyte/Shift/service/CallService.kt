package com.Linkbyte.Shift.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.Linkbyte.Shift.MainActivity
import com.Linkbyte.Shift.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CallService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "call_service_channel"
        val channel = NotificationChannel(
            channelId,
            "Active Call",
            NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Shift Call")
            .setContentText("Ongoing call...")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
        
        return START_NOT_STICKY
    }
}
