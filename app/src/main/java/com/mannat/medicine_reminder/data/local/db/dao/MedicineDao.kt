package com.mannat.medicine_reminder.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mannat.medicine_reminder.data.local.db.entity.MedicineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {

    @Query("SELECT * FROM medicines WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines ORDER BY name ASC")
    fun getAllMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: Long): MedicineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @Query("UPDATE medicines SET isActive = 0, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteMedicine(id: Long, now: Long)

    @Delete
    suspend fun hardDeleteMedicine(medicine: MedicineEntity)
}
