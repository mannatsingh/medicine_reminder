package com.mannat.medicine_reminder.domain.usecase.medicine

import com.mannat.medicine_reminder.domain.model.Medicine
import com.mannat.medicine_reminder.domain.repository.MedicineRepository
import com.mannat.medicine_reminder.notification.AlarmScheduler
import javax.inject.Inject

class UpdateMedicineUseCase @Inject constructor(
    private val repository: MedicineRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(medicine: Medicine) {
        // Cancel old alarms before updating
        val oldMedicine = repository.getMedicineById(medicine.id)
        oldMedicine?.schedules?.forEach { schedule ->
            alarmScheduler.cancelAlarm(schedule.id)
        }

        repository.updateMedicine(medicine)

        // Schedule new alarms with the updated schedule IDs
        val updated = repository.getMedicineById(medicine.id)
        updated?.schedules?.forEach { schedule ->
            alarmScheduler.scheduleAlarm(schedule.id, schedule.time.hour, schedule.time.minute)
        }
    }
}
