package com.mannat.medicine_reminder.domain.usecase.doselog

import com.mannat.medicine_reminder.domain.model.AdherenceStats
import com.mannat.medicine_reminder.domain.model.DayAdherence
import com.mannat.medicine_reminder.domain.model.DoseStatus
import com.mannat.medicine_reminder.domain.repository.DoseLogRepository
import com.mannat.medicine_reminder.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
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

            val today = LocalDate.now()
            // Don't count future days
            val effectiveEnd = if (endDate.isAfter(today)) today else endDate

            // Compute daily breakdown — only for days that actually had scheduled doses
            // and only from when each medicine was created
            val dailyBreakdown = mutableListOf<DayAdherence>()
            var date = startDate
            while (!date.isAfter(effectiveEnd)) {
                val dayOfWeek = date.dayOfWeek

                // Only count medicines that existed on this date
                val activeMedicinesForDay = filteredMedicines.filter { medicine ->
                    val createdDate = medicine.createdAt
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    dayOfWeek in medicine.activeDays && !date.isBefore(createdDate)
                }

                val scheduledCount = activeMedicinesForDay.sumOf { it.schedules.size }

                if (scheduledCount > 0) {
                    val dayLogs = logs.filter { it.date == date }
                    val takenCount = dayLogs.count { it.status == DoseStatus.TAKEN }

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

            // Compute streaks (consecutive days with 100% adherence, working backward)
            var currentStreak = 0
            var longestStreak = 0
            var tempStreak = 0

            for (day in dailyBreakdown.sortedByDescending { it.date }) {
                if (day.takenCount == day.scheduledCount) {
                    tempStreak++
                    if (currentStreak == tempStreak - 1) {
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

            // Default to 100% when there's no data yet (new user, no doses scheduled yet)
            val adherencePercentage = when {
                totalScheduled == 0 -> 100f
                else -> (totalTaken.toFloat() / totalScheduled) * 100f
            }

            AdherenceStats(
                totalScheduledDoses = totalScheduled,
                takenDoses = totalTaken,
                missedDoses = totalMissed,
                skippedDoses = totalSkipped,
                adherencePercentage = adherencePercentage,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                dailyBreakdown = dailyBreakdown
            )
        }
    }
}
