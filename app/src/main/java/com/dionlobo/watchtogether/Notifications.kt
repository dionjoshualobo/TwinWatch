package com.dionlobo.watchtogether

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

private const val CHANNEL_ID = "watch_together_activity"

class WatchMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val author = message.data["authorUid"] ?: message.data["uid"] ?: ""
        if (author.isNotEmpty() && author == Repository().currentUid()) return

        val body = message.data["body"]
            ?: message.notification?.body
            ?: "Something new happened in your group"
        showActivityNotification(this, body, message.data["groupId"] ?: "")
    }
}

fun showActivityNotification(context: Context, body: String, groupId: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Group activity",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Episodes and notes from your groups" }
        manager.createNotificationChannel(channel)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (groupId.isNotEmpty()) putExtra("groupId", groupId)
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        groupId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Watch Together")
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .build()

    manager.notify((groupId + body).hashCode(), notification)
}
