package com.mannat.medicine_reminder.domain.usecase.alarm

import com.mannat.medicine_reminder.data.local.db.dao.MedicineDao
import com.mannat.medicine_reminder.data.local.db.dao.ScheduleDao
import com.mannat.medicine_reminder.notification.AlarmScheduler
import javax.inject.Inject

class RescheduleAllAlarmsUseCase @Inject constructor(
    private val medicineDao: MedicineDao,
    private val scheduleDao: ScheduleDao,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke() {
        val schedules = scheduleDao.getAllSchedules()
        val activeMedicineIds = mutableSetOf<Long>()

        // Build set of active medicine IDs
        schedules.map { it.medicineId }.distinct().forEach { medicineId ->
            val medicine = medicineDao.getMedicineById(medicineId)
            if (medicine?.isActive == true) {
                activeMedicineIds.add(medicineId)
            }
        }

        // Schedule alarms only for active medicines
        schedules
            .filter { it.medicineId in activeMedicineIds }
            .forEach { schedule ->
                alarmScheduler.scheduleAlarm(schedule.id, schedule.hour, schedule.minute)
            }
    }
}
