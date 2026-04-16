package com.mannat.medicine_reminder.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mannat.medicine_reminder.data.local.db.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedules WHERE medicineId = :medicineId ORDER BY hour, minute")
    fun getSchedulesForMedicine(medicineId: Long): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE medicineId = :medicineId ORDER BY hour, minute")
    suspend fun getSchedulesForMedicineOnce(medicineId: Long): List<ScheduleEntity>

    @Query("SELECT * FROM schedules")
    suspend fun getAllSchedules(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ScheduleEntity>): List<Long>

    @Query("DELETE FROM schedules WHERE medicineId = :medicineId")
    suspend fun deleteSchedulesForMedicine(medicineId: Long)
}
