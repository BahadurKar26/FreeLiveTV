package com.rancon.freelivetv.data

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val reminderDao = database.reminderDao()
        val programDao = database.programDao()
        val channelDao = database.channelDao()
        
        val now = System.currentTimeMillis()
        // Check for reminders starting in the next 10 minutes that haven't been notified
        val reminders = reminderDao.getAllReminders().first().filter { 
            !it.isNotified && it.startTime > now && it.startTime <= (now + 10 * 60 * 1000)
        }

        val notificationHelper = NotificationHelper(applicationContext)

        reminders.forEach { reminder ->
            val channel = channelDao.getChannelById(reminder.channelId)
            notificationHelper.showReminderNotification(
                programTitle = reminder.programTitle,
                channelId = reminder.channelId,
                channelName = channel?.name ?: "Unknown Channel"
            )
            reminderDao.markAsNotified(reminder.programId)
        }

        // Cleanup old reminders while we're at it
        reminderDao.cleanupOldReminders(now - 24 * 60 * 60 * 1000)

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "ReminderCheckWorker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val request = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
