package com.mannat.medicine_reminder.domain.usecase.doselog

import com.mannat.medicine_reminder.domain.model.AdherenceStats
import com.mannat.medicine_reminder.domain.model.DayAdherence
import com.mannat.medicine_reminder.domain.model.DoseStatus
import com.mannat.medicine_reminder.domain.repository.DoseLogRepository
import com.mannat.medicine_reminder.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

class GetAdherenceStatsUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val doseLogRepository: DoseLogRepository
) {
    fun invoke(
        medicineId: Long?,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<AdherenceStats> {
        val medicinesFlow = medicineRepository.getAllMedicines()
        val logsFlow = if (medicineId != null) {
            doseLogRepository.getDoseLogsForMedicineInRange(medicineId, startDate, endDate)
        } else {
            doseLogRepository.getAllDoseLogsInRange(startDate, endDate)
        }

        return combine(medicinesFlow, logsFlow) { medicines, logs ->
            val filteredMedicines = if (medicineId != null) {
                medicines.filter { it.id == medicineId }
            } else {
                medicines
            }

            // Compute daily breakdown
            val dailyBreakdown = mutableListOf<DayAdherence>()
            var date = startDate
            while (!date.isAfter(endDate)) {
                val dayOfWeek = date.dayOfWeek

                // Count expected doses for this date
                val scheduledCount = filteredMedicines
                    .filter { dayOfWeek in it.activeDays }
                    .sumOf { it.schedules.size }

                // Count taken doses for this date
                val dayLogs = logs.filter { it.date == date }
                val takenCount = dayLogs.count { it.status == DoseStatus.TAKEN }

                if (scheduledCount > 0) {
                    dailyBreakdown.add(
                        DayAdherence(
                            date = date,
                            scheduledCount = scheduledCount,
                            takenCount = takenCount,
                            adherencePercentage = (takenCount.toFloat() / scheduledCount) * 100f
                        )
                    )
                }

                date = date.plusDays(1)
            }

            // Compute streaks (consecutive days with 100% adherence, working backward from today)
            val today = LocalDate.now()
            var currentStreak = 0
            var longestStreak = 0
            var tempStreak = 0

            for (day in dailyBreakdown.sortedByDescending { it.date }) {
                if (day.scheduledCount > 0 && day.takenCount == day.scheduledCount) {
                    tempStreak++
                    if (day.date <= today && currentStreak == tempStreak - 1) {
                        currentStreak = tempStreak
                    }
                } else {
                    longestStreak = maxOf(longestStreak, tempStreak)
                    tempStreak = 0
                }
            }
            longestStreak = maxOf(longestStreak, tempStreak)

            val totalScheduled = dailyBreakdown.sumOf { it.scheduledCount }
            val totalTaken = logs.count { it.status == DoseStatus.TAKEN }
            val totalMissed = logs.count { it.status == DoseStatus.MISSED }
            val totalSkipped = logs.count { it.status == DoseStatus.SKIPPED }

            AdherenceStats(
                totalScheduledDoses = totalScheduled,
                takenDoses = totalTaken,
                missedDoses = totalMissed,
                skippedDoses = totalSkipped,
                adherencePercentage = if (totalScheduled > 0) (totalTaken.toFloat() / totalScheduled) * 100f else 0f,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                dailyBreakdown = dailyBreakdown
            )
        }
    }
}
