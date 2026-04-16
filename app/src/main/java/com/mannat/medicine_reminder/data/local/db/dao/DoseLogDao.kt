package com.mannat.medicine_reminder.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mannat.medicine_reminder.data.local.db.entity.DoseLogEntity
import com.mannat.medicine_reminder.data.local.db.entity.DoseLogWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {

    @Query(
        """
        SELECT dl.id, dl.scheduleId, dl.date, dl.takenAt, dl.status,
               s.hour, s.minute, s.medicineId, m.name, m.dosage
        FROM dose_logs dl
        INNER JOIN schedules s ON dl.scheduleId = s.id
        INNER JOIN medicines m ON s.medicineId = m.id
        WHERE dl.date = :date
        ORDER BY s.hour, s.minute
        """
    )
    fun getDoseLogsForDate(date: String): Flow<List<DoseLogWithDetails>>

    @Query(
        """
        SELECT dl.* FROM dose_logs dl
        INNER JOIN schedules s ON dl.scheduleId = s.id
        WHERE s.medicineId = :medicineId AND dl.date BETWEEN :startDate AND :endDate
        """
    )
    fun getDoseLogsForMedicineInRange(
        medicineId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<DoseLogEntity>>

    @Query(
        """
        SELECT dl.* FROM dose_logs dl
        WHERE dl.date BETWEEN :startDate AND :endDate
        """
    )
    fun getAllDoseLogsInRange(
        startDate: String,
        endDate: String
    ): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE scheduleId = :scheduleId AND date = :date LIMIT 1")
    suspend fun getDoseLog(scheduleId: Long, date: String): DoseLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDoseLog(log: DoseLogEntity)

    @Query("DELETE FROM dose_logs WHERE scheduleId = :scheduleId AND date = :date")
    suspend fun deleteDoseLog(scheduleId: Long, date: String)

    @Query(
        """
        SELECT COUNT(*) FROM dose_logs dl
        INNER JOIN schedules s ON dl.scheduleId = s.id
        WHERE s.medicineId = :medicineId AND dl.status = 'TAKEN'
          AND dl.date BETWEEN :startDate AND :endDate
        """
    )
    suspend fun countTakenDoses(medicineId: Long, startDate: String, endDate: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM dose_logs dl
        INNER JOIN schedules s ON dl.scheduleId = s.id
        WHERE s.medicineId = :medicineId
          AND dl.date BETWEEN :startDate AND :endDate
        """
    )
    suspend fun countTotalLoggedDoses(medicineId: Long, startDate: String, endDate: String): Int

    @Query(
        """
        DELETE FROM dose_logs WHERE scheduleId IN (
            SELECT s.id FROM schedules s WHERE s.medicineId = :medicineId
        )
        """
    )
    suspend fun deleteAllLogsForMedicine(medicineId: Long)
}
