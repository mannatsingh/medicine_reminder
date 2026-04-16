package com.mannat.medicine_reminder.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager: AlarmManager? = context.getSystemService()

    companion object {
        private const val FOLLOW_UP_REQUEST_CODE_OFFSET = 500_000
    }

    fun scheduleAlarm(scheduleId: Long, hour: Int, minute: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager?.canScheduleExactAlarms() == false) {
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = calculateNextTriggerTime(hour, minute)

        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    fun scheduleFollowUpAlarm(scheduleId: Long, delayMinutes: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager?.canScheduleExactAlarms() == false) {
            return
        }

        val intent = Intent(context, FollowUpAlarmReceiver::class.java).apply {
            putExtra(FollowUpAlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt() + FOLLOW_UP_REQUEST_CODE_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)

        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    fun cancelAlarm(scheduleId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager?.cancel(it) }

        // Also cancel any follow-up
        cancelFollowUpAlarm(scheduleId)
    }

    private fun cancelFollowUpAlarm(scheduleId: Long) {
        val intent = Intent(context, FollowUpAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt() + FOLLOW_UP_REQUEST_CODE_OFFSET,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager?.cancel(it) }
    }

    fun cancelAlarmsForScheduleIds(scheduleIds: List<Long>) {
        scheduleIds.forEach { cancelAlarm(it) }
    }

    private fun calculateNextTriggerTime(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now()
        var target = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return target.toInstant().toEpochMilli()
    }
}
