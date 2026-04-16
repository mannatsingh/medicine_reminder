package com.mannat.medicine_reminder.data.local.db.entity

data class DoseLogWithDetails(
    val id: Long,
    val scheduleId: Long,
    val date: String,
    val takenAt: Long?,
    val status: String,
    val hour: Int,
    val minute: Int,
    val medicineId: Long,
    val name: String,
    val dosage: String
)
