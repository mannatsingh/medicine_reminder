package com.mannat.medicine_reminder.data.mapper

import com.mannat.medicine_reminder.data.local.db.entity.DoseLogEntity
import com.mannat.medicine_reminder.data.local.db.entity.DoseLogWithDetails
import com.mannat.medicine_reminder.domain.model.DailyDoseItem
import com.mannat.medicine_reminder.domain.model.DoseLog
import com.mannat.medicine_reminder.domain.model.DoseStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun DoseLogEntity.toDomain(): DoseLog {
    return DoseLog(
        id = id,
        scheduleId = scheduleId,
        date = LocalDate.parse(date),
        takenAt = takenAt?.let { Instant.ofEpochMilli(it) },
        status = DoseStatus.valueOf(status)
    )
}

fun DoseLog.toEntity(): DoseLogEntity {
    return DoseLogEntity(
        id = id,
        scheduleId = scheduleId,
        date = date.toString(),
        takenAt = takenAt?.toEpochMilli(),
        status = status.name
    )
}

fun DoseLogWithDetails.toDailyDoseItem(): DailyDoseItem {
    return DailyDoseItem(
        scheduleId = scheduleId,
        medicineId = medicineId,
        medicineName = name,
        dosage = dosage,
        scheduledTime = LocalTime.of(hour, minute),
        status = DoseStatus.valueOf(status),
        takenAt = takenAt?.let { Instant.ofEpochMilli(it) }
    )
}
