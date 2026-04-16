package com.mannat.medicine_reminder.domain.repository

import com.mannat.medicine_reminder.domain.model.Medicine
import kotlinx.coroutines.flow.Flow

interface MedicineRepository {
    fun getAllActiveMedicines(): Flow<List<Medicine>>
    fun getAllMedicines(): Flow<List<Medicine>>
    suspend fun getMedicineById(id: Long): Medicine?
    suspend fun addMedicine(medicine: Medicine): Long
    suspend fun updateMedicine(medicine: Medicine)
    suspend fun deleteMedicine(id: Long)
}
