package com.chasinglemons.empeg.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chasinglemons.empeg.R
import com.chasinglemons.empeg.main.MainActivity

object PlayerNotification {
    const val CHANNEL_ID = "empeg_remote"
    private const val CHANNEL_NAME = "Empeg Remote"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing control shortcut for the connected empeg"
            setShowBadge(false)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun show(context: Context, empegIp: String) {
        createChannel(context)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (empegIp.isBlank()) {
            context.getString(R.string.notification_content_disconnected)
        } else {
            context.getString(R.string.notification_content_connected, empegIp)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play_circle)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
