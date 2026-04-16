package com.mannat.medicine_reminder.domain.usecase.doselog

import com.mannat.medicine_reminder.domain.model.DailyDoseItem
import com.mannat.medicine_reminder.domain.model.DoseStatus
import com.mannat.medicine_reminder.domain.repository.DoseLogRepository
import com.mannat.medicine_reminder.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

class GetDoseLogsForDateUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val doseLogRepository: DoseLogRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<DailyDoseItem>> {
        return combine(
            medicineRepository.getAllActiveMedicines(),
            doseLogRepository.getDailyDoseItems(date)
        ) { medicines, loggedItems ->
            val dayOfWeek = date.dayOfWeek

            // Build expected dose items from active medicines whose activeDays include this date
            val expectedItems = medicines
                .filter { dayOfWeek in it.activeDays }
                .flatMap { medicine ->
                    medicine.schedules.map { schedule ->
                        DailyDoseItem(
                            scheduleId = schedule.id,
                            medicineId = medicine.id,
                            medicineName = medicine.name,
                            dosage = medicine.dosage,
                            scheduledTime = schedule.time,
                            status = null,
                            takenAt = null
                        )
                    }
                }

            // Merge with actual logs: if a log exists for a scheduleId, use its status
            val logMap = loggedItems.associateBy { it.scheduleId }

            expectedItems.map { item ->
                val log = logMap[item.scheduleId]
                if (log != null) {
                    item.copy(status = log.status, takenAt = log.takenAt)
                } else {
                    item
                }
            }.sortedBy { it.scheduledTime }
        }
    }
}
