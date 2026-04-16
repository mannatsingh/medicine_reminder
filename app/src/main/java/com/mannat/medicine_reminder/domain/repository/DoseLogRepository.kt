package com.mannat.medicine_reminder.domain.repository

import com.mannat.medicine_reminder.domain.model.DailyDoseItem
import com.mannat.medicine_reminder.domain.model.DoseLog
import com.mannat.medicine_reminder.domain.model.DoseStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DoseLogRepository {
    fun getDailyDoseItems(date: LocalDate): Flow<List<DailyDoseItem>>
    suspend fun logDose(scheduleId: Long, date: LocalDate, status: DoseStatus)
    suspend fun undoLogDose(scheduleId: Long, date: LocalDate)
    fun getDoseLogsForMedicineInRange(
        medicineId: Long,
        start: LocalDate,
        end: LocalDate
    ): Flow<List<DoseLog>>
    fun getAllDoseLogsInRange(start: LocalDate, end: LocalDate): Flow<List<DoseLog>>
}
