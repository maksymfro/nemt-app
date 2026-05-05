package com.mobapps.nemt.notifications

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class TripReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val title = inputData.getString(NemtNotifications.INPUT_TITLE)
            ?: "Upcoming ride reminder"
        val body = inputData.getString(NemtNotifications.INPUT_BODY)
            ?: "You have an upcoming ride."
        val type = inputData.getString(NemtNotifications.INPUT_TYPE)
            ?.let { runCatching { NemtNotificationType.valueOf(it) }.getOrNull() }
            ?: NemtNotificationType.TRIP_REMINDER
        NemtNotifications.notifyNow(applicationContext, type, title, body)
        return Result.success()
    }
}

