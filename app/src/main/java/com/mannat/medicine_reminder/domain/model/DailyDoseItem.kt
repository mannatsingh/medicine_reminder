package com.mannat.medicine_reminder.domain.model

import java.time.Instant
import java.time.LocalTime

data class DailyDoseItem(
    val scheduleId: Long,
    val medicineId: Long,
    val medicineName: String,
    val dosage: String,
    val scheduledTime: LocalTime,
    val status: DoseStatus?,
    val takenAt: Instant? = null
)
