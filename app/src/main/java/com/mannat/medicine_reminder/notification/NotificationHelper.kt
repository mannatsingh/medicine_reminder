package com.mannat.medicine_reminder.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.mannat.medicine_reminder.R
import com.mannat.medicine_reminder.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "dose_reminders"
        const val CHANNEL_NAME = "Dose Reminders"
        private const val ACTION_PENDING_INTENT_OFFSET = 100_000
    }

    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders to take your medicine"
            enableVibration(true)
        }
        context.getSystemService<NotificationManager>()
            ?.createNotificationChannel(channel)
    }

    fun showDoseReminder(
        scheduleId: Long,
        medicineName: String,
        dosage: String,
        hour: Int,
        minute: Int
    ) {
        // Tap notification -> open app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Taken" action button
        val takenIntent = Intent(context, DoseActionReceiver::class.java).apply {
            action = DoseActionReceiver.ACTION_DOSE_TAKEN
            putExtra(DoseActionReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt() + ACTION_PENDING_INTENT_OFFSET,
            takenIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeStr = formatTime(hour, minute)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle("Time for $medicineName")
            .setContentText("$dosage - $timeStr")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Taken", takenPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(scheduleId.toInt(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    fun showFollowUpReminder(
        scheduleId: Long,
        medicineName: String,
        dosage: String,
        notificationId: Int
    ) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, notificationId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takenIntent = Intent(context, DoseActionReceiver::class.java).apply {
            action = DoseActionReceiver.ACTION_DOSE_TAKEN
            putExtra(DoseActionReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 100_000, takenIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle("Don't forget: $medicineName")
            .setContentText("You haven't taken $dosage yet")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Taken", takenPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) { }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val displayHour = if (hour % 12 == 0) 12 else hour % 12
        val amPm = if (hour < 12) "AM" else "PM"
        return "%d:%02d %s".format(displayHour, minute, amPm)
    }
}
