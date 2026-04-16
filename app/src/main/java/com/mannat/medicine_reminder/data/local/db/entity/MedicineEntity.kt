package com.mannat.medicine_reminder.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val frequency: String,
    val activeDays: String,
    val notes: String? = null,
    val isActive: Boolean = true,
    val reminderWindowMinutes: Int? = null,
    val notificationsEnabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)
