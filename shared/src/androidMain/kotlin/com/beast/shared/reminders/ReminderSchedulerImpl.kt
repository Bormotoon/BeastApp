package com.beast.shared.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "beast_reminders"
private const val WORK_TAG = "beast_app_reminder"

class ReminderSchedulerImpl(
    private val appContext: Context
) : ReminderScheduler {
    override fun scheduleAt(timestampMillis: Long, title: String, message: String) {
        ensureChannel()
        val delay = (timestampMillis - System.currentTimeMillis()).coerceAtLeast(0)
        val data = workDataOf(
            "title" to title,
            "message" to message
        )
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(appContext).enqueue(request)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Workout Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "Workout Reminder"
        val message = inputData.getString("message") ?: "Time to train!"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        return Result.success()
    }
}
