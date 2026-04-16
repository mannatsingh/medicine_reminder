package com.mannat.medicine_reminder.domain.usecase.medicine

import com.mannat.medicine_reminder.domain.model.Medicine
import com.mannat.medicine_reminder.domain.repository.MedicineRepository
import com.mannat.medicine_reminder.notification.AlarmScheduler
import javax.inject.Inject

class AddMedicineUseCase @Inject constructor(
    private val repository: MedicineRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(medicine: Medicine): Long {
        val medicineId = repository.addMedicine(medicine)

        // Fetch the saved medicine to get the generated schedule IDs
        val saved = repository.getMedicineById(medicineId)
        saved?.schedules?.forEach { schedule ->
            alarmScheduler.scheduleAlarm(schedule.id, schedule.time.hour, schedule.time.minute)
        }

        return medicineId
    }
}
