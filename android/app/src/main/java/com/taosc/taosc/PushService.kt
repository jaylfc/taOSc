package com.taosc.taosc

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class PushService : Service() {
    companion object {
        const val ACTION_START = "com.taosc.taosc.action.START_PUSH"
        const val ACTION_STOP = "com.taosc.taosc.action.STOP_PUSH"
        const val NOTIFICATION_ID = 2001
    }
    
    override fun onBind(intent: Intent): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForeground(NOTIFICATION_ID, buildForegroundNotification())
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    private fun buildForegroundNotification(): Notification {
        val channelId = "push_service"
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("taOSc")
            .setContentText("Push service running")
            .setPriority(NotificationCompat.PRIORITY_LOW)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Push Service",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        
        return builder.build()
    }
}
