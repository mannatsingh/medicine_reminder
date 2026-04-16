package com.mannat.medicine_reminder.domain.usecase.medicine

import com.mannat.medicine_reminder.domain.repository.MedicineRepository
import com.mannat.medicine_reminder.notification.AlarmScheduler
import javax.inject.Inject

class DeleteMedicineUseCase @Inject constructor(
    private val repository: MedicineRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(id: Long) {
        // Cancel alarms before deleting
        val medicine = repository.getMedicineById(id)
        medicine?.schedules?.forEach { schedule ->
            alarmScheduler.cancelAlarm(schedule.id)
        }

        repository.deleteMedicine(id)
    }
}
