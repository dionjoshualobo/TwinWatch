package com.dionlobo.watchtogether

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val CHANNEL_ID = "watch_together_reminders"
    private const val KEY_SLOT = "slot"
    private const val KEY_TITLE = "title"
    private const val KEY_BODY = "body"

    fun schedule(context: Context) {
        scheduleSlot(context, "midday", 13, 0, "Watch check-in", "Did you watch anything today? Tap to log it.")
        scheduleSlot(context, "evening", 21, 0, "Tonight's recap", "Log the episodes you finished tonight.")
    }

    fun scheduleSlot(context: Context, slot: String, hour: Int, minute: Int, title: String, body: String) {
        val work = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(millisUntil(hour, minute), TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    KEY_SLOT to slot,
                    KEY_TITLE to title,
                    KEY_BODY to body
                )
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("watch_reminder_$slot", ExistingWorkPolicy.REPLACE, work)
    }

    private fun millisUntil(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val due = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (due.timeInMillis <= now.timeInMillis) due.add(Calendar.DATE, 1)
        return due.timeInMillis - now.timeInMillis
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Watch reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "Daily prompts to log watched episodes"
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, slot: String, title: String, body: String) {
        createChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openLog", true)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pending = PendingIntent.getActivity(context, slot.hashCode(), intent, flags)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify(slot.hashCode(), notification)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val slot = inputData.getString("slot") ?: "midday"
        val title = inputData.getString("title") ?: "Watch check-in"
        val body = inputData.getString("body") ?: "Did you watch anything today? Tap to log it."
        ReminderScheduler.show(applicationContext, slot, title, body)
        val hour = if (slot == "evening") 21 else 13
        val minute = 0
        ReminderScheduler.scheduleSlot(applicationContext, slot, hour, minute, title, body)
        return Result.success()
    }
}
