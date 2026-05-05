package com.mobapps.nemt.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mobapps.nemt.MainActivity
import com.mobapps.nemt.R
import java.util.concurrent.TimeUnit

enum class NemtNotificationType {
    TRIP_CONFIRMED,
    TRIP_REMINDER,
    TRIP_UPDATED,
    TRIP_CANCELLED,
    SUPPORT_CONTACT
}

object NemtNotifications {
    const val CHANNEL_TRIP_UPDATES = "trip_updates"
    const val CHANNEL_REMINDERS = "upcoming_reminders"
    const val CHANNEL_SUPPORT = "support_alerts"

    const val INPUT_TITLE = "title"
    const val INPUT_BODY = "body"
    const val INPUT_TYPE = "type"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(
                CHANNEL_TRIP_UPDATES,
                "Trip updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ),
            NotificationChannel(
                CHANNEL_REMINDERS,
                "Upcoming reminders",
                NotificationManager.IMPORTANCE_HIGH
            ),
            NotificationChannel(
                CHANNEL_SUPPORT,
                "Support alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        channels.forEach(nm::createNotificationChannel)
    }

    fun notifyNow(
        context: Context,
        type: NemtNotificationType,
        title: String,
        body: String
    ) {
        ensureChannels(context)
        val channelId = when (type) {
            NemtNotificationType.TRIP_CONFIRMED,
            NemtNotificationType.TRIP_UPDATED,
            NemtNotificationType.TRIP_CANCELLED -> CHANNEL_TRIP_UPDATES
            NemtNotificationType.TRIP_REMINDER -> CHANNEL_REMINDERS
            NemtNotificationType.SUPPORT_CONTACT -> CHANNEL_SUPPORT
        }
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            type.ordinal,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setColor(context.getColor(R.color.purple_500))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(
            (type.name + title + body).hashCode(),
            n
        )
    }

    fun scheduleTripReminder(
        context: Context,
        requestKey: String,
        scheduledAtMillis: Long,
        destination: String
    ) {
        val delayMs = scheduledAtMillis - 15 * 60_000L - System.currentTimeMillis()
        if (delayMs <= 5_000L) return
        val data = Data.Builder()
            .putString(INPUT_TITLE, "Upcoming ride reminder")
            .putString(INPUT_BODY, "Pickup is in ~15 minutes. Destination: $destination")
            .putString(INPUT_TYPE, NemtNotificationType.TRIP_REMINDER.name)
            .build()
        val req = OneTimeWorkRequestBuilder<TripReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "trip_reminder_$requestKey",
            androidx.work.ExistingWorkPolicy.REPLACE,
            req
        )
    }
}

