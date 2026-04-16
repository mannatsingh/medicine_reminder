package com.mannat.medicine_reminder.data.repository

import com.mannat.medicine_reminder.data.local.db.dao.DoseLogDao
import com.mannat.medicine_reminder.data.local.db.entity.DoseLogEntity
import com.mannat.medicine_reminder.data.mapper.toDailyDoseItem
import com.mannat.medicine_reminder.data.mapper.toDomain
import com.mannat.medicine_reminder.domain.model.DailyDoseItem
import com.mannat.medicine_reminder.domain.model.DoseLog
import com.mannat.medicine_reminder.domain.model.DoseStatus
import com.mannat.medicine_reminder.domain.repository.DoseLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class DoseLogRepositoryImpl @Inject constructor(
    private val doseLogDao: DoseLogDao
) : DoseLogRepository {

    override fun getDailyDoseItems(date: LocalDate): Flow<List<DailyDoseItem>> {
        return doseLogDao.getDoseLogsForDate(date.toString()).map { list ->
            list.map { it.toDailyDoseItem() }
        }
    }

    override suspend fun logDose(scheduleId: Long, date: LocalDate, status: DoseStatus) {
        val existing = doseLogDao.getDoseLog(scheduleId, date.toString())
        val entity = DoseLogEntity(
            id = existing?.id ?: 0,
            scheduleId = scheduleId,
            date = date.toString(),
            takenAt = if (status == DoseStatus.TAKEN) Instant.now().toEpochMilli() else null,
            status = status.name
        )
        doseLogDao.upsertDoseLog(entity)
    }

    override suspend fun undoLogDose(scheduleId: Long, date: LocalDate) {
        doseLogDao.deleteDoseLog(scheduleId, date.toString())
    }

    override fun getDoseLogsForMedicineInRange(
        medicineId: Long,
        start: LocalDate,
        end: LocalDate
    ): Flow<List<DoseLog>> {
        return doseLogDao.getDoseLogsForMedicineInRange(
            medicineId, start.toString(), end.toString()
        ).map { list -> list.map { it.toDomain() } }
    }

    override fun getAllDoseLogsInRange(
        start: LocalDate,
        end: LocalDate
    ): Flow<List<DoseLog>> {
        return doseLogDao.getAllDoseLogsInRange(
            start.toString(), end.toString()
        ).map { list -> list.map { it.toDomain() } }
    }
}
