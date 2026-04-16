package com.mannat.medicine_reminder.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mannat.medicine_reminder.data.local.db.dao.DoseLogDao
import com.mannat.medicine_reminder.data.local.db.dao.MedicineDao
import com.mannat.medicine_reminder.data.local.db.dao.ScheduleDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class FollowUpAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduleDao: ScheduleDao
    @Inject lateinit var medicineDao: MedicineDao
    @Inject lateinit var doseLogDao: DoseLogDao
    @Inject lateinit var notificationHelper: NotificationHelper

    companion object {
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        private const val FOLLOW_UP_NOTIFICATION_OFFSET = 200_000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1)
        if (scheduleId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if dose was already taken
                val today = LocalDate.now().toString()
                val existingLog = doseLogDao.getDoseLog(scheduleId, today)
                if (existingLog != null) return@launch // Already handled

                val schedule = scheduleDao.getScheduleById(scheduleId) ?: return@launch
                val medicine = medicineDao.getMedicineById(schedule.medicineId) ?: return@launch
                if (!medicine.isActive || !medicine.notificationsEnabled) return@launch

                notificationHelper.showFollowUpReminder(
                    scheduleId = scheduleId,
                    medicineName = medicine.name,
                    dosage = medicine.dosage,
                    notificationId = scheduleId.toInt() + FOLLOW_UP_NOTIFICATION_OFFSET
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
