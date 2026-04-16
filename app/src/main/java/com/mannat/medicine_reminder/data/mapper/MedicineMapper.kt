package com.mannat.medicine_reminder.data.mapper

import com.mannat.medicine_reminder.data.local.db.entity.MedicineEntity
import com.mannat.medicine_reminder.data.local.db.entity.ScheduleEntity
import com.mannat.medicine_reminder.domain.model.Frequency
import com.mannat.medicine_reminder.domain.model.Medicine
import com.mannat.medicine_reminder.domain.model.Schedule
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

fun MedicineEntity.toDomain(schedules: List<ScheduleEntity> = emptyList()): Medicine {
    return Medicine(
        id = id,
        name = name,
        dosage = dosage,
        frequency = Frequency.valueOf(frequency),
        activeDays = activeDays.split(",")
            .filter { it.isNotBlank() }
            .map { DayOfWeek.of(it.trim().toInt()) }
            .toSet(),
        schedules = schedules.map { it.toDomain() },
        notes = notes,
        isActive = isActive,
        reminderWindowMinutes = reminderWindowMinutes,
        notificationsEnabled = notificationsEnabled,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt)
    )
}

fun Medicine.toEntity(): MedicineEntity {
    return MedicineEntity(
        id = id,
        name = name,
        dosage = dosage,
        frequency = frequency.name,
        activeDays = activeDays.joinToString(",") { it.value.toString() },
        notes = notes,
        isActive = isActive,
        reminderWindowMinutes = reminderWindowMinutes,
        notificationsEnabled = notificationsEnabled,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli()
    )
}

fun ScheduleEntity.toDomain(): Schedule {
    return Schedule(
        id = id,
        medicineId = medicineId,
        time = LocalTime.of(hour, minute)
    )
}

fun Schedule.toEntity(medicineId: Long): ScheduleEntity {
    return ScheduleEntity(
        id = id,
        medicineId = medicineId,
        hour = time.hour,
        minute = time.minute
    )
}
