package com.mannat.medicine_reminder.data.repository

import com.mannat.medicine_reminder.data.local.db.dao.MedicineDao
import com.mannat.medicine_reminder.data.local.db.dao.ScheduleDao
import com.mannat.medicine_reminder.data.mapper.toDomain
import com.mannat.medicine_reminder.data.mapper.toEntity
import com.mannat.medicine_reminder.domain.model.Medicine
import com.mannat.medicine_reminder.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class MedicineRepositoryImpl @Inject constructor(
    private val medicineDao: MedicineDao,
    private val scheduleDao: ScheduleDao
) : MedicineRepository {

    override fun getAllActiveMedicines(): Flow<List<Medicine>> {
        return medicineDao.getAllActiveMedicines().map { entities ->
            entities.map { entity ->
                val schedules = scheduleDao.getSchedulesForMedicineOnce(entity.id)
                entity.toDomain(schedules)
            }
        }
    }

    override fun getAllMedicines(): Flow<List<Medicine>> {
        return medicineDao.getAllMedicines().map { entities ->
            entities.map { entity ->
                val schedules = scheduleDao.getSchedulesForMedicineOnce(entity.id)
                entity.toDomain(schedules)
            }
        }
    }

    override suspend fun getMedicineById(id: Long): Medicine? {
        val entity = medicineDao.getMedicineById(id) ?: return null
        val schedules = scheduleDao.getSchedulesForMedicineOnce(id)
        return entity.toDomain(schedules)
    }

    override suspend fun addMedicine(medicine: Medicine): Long {
        val now = Instant.now()
        val entity = medicine.copy(createdAt = now, updatedAt = now).toEntity()
        val medicineId = medicineDao.insertMedicine(entity)

        val scheduleEntities = medicine.schedules.map { it.toEntity(medicineId) }
        scheduleDao.insertSchedules(scheduleEntities)

        return medicineId
    }

    override suspend fun updateMedicine(medicine: Medicine) {
        val now = Instant.now()
        val entity = medicine.copy(updatedAt = now).toEntity()
        medicineDao.updateMedicine(entity)

        // Replace all schedules
        scheduleDao.deleteSchedulesForMedicine(medicine.id)
        val scheduleEntities = medicine.schedules.map { it.toEntity(medicine.id) }
        scheduleDao.insertSchedules(scheduleEntities)
    }

    override suspend fun deleteMedicine(id: Long) {
        medicineDao.softDeleteMedicine(id, Instant.now().toEpochMilli())
    }
}
