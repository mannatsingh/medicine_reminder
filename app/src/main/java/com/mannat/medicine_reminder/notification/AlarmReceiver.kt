package com.mannat.medicine_reminder.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mannat.medicine_reminder.data.local.datastore.PreferencesManager
import com.mannat.medicine_reminder.data.local.db.dao.MedicineDao
import com.mannat.medicine_reminder.data.local.db.dao.ScheduleDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduleDao: ScheduleDao
    @Inject lateinit var medicineDao: MedicineDao
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var preferencesManager: PreferencesManager

    companion object {
        const val EXTRA_SCHEDULE_ID = "schedule_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1)
        if (scheduleId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val schedule = scheduleDao.getScheduleById(scheduleId) ?: return@launch
                val medicine = medicineDao.getMedicineById(schedule.medicineId) ?: return@launch

                if (!medicine.isActive) return@launch

                // Check if today is an active day for this medicine
                val todayValue = LocalDate.now().dayOfWeek.value.toString()
                if (todayValue in medicine.activeDays.split(",")) {
                    // Only show notification if medicine has notifications enabled
                    if (medicine.notificationsEnabled) {
                        notificationHelper.showDoseReminder(
                            scheduleId = scheduleId,
                            medicineName = medicine.name,
                            dosage = medicine.dosage,
                            hour = schedule.hour,
                            minute = schedule.minute
                        )

                        // Schedule follow-up reminder
                        val windowMinutes = medicine.reminderWindowMinutes
                            ?: preferencesManager.defaultReminderWindowMinutes.first()
                        if (windowMinutes > 0) {
                            alarmScheduler.scheduleFollowUpAlarm(scheduleId, windowMinutes)
                        }
                    }
                }

                // Reschedule for next day
                alarmScheduler.scheduleAlarm(scheduleId, schedule.hour, schedule.minute)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
