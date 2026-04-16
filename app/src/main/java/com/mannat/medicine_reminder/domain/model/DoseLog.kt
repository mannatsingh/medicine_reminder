package com.mannat.medicine_reminder.domain.model

import java.time.Instant
import java.time.LocalDate

data class DoseLog(
    val id: Long = 0,
    val scheduleId: Long,
    val date: LocalDate,
    val takenAt: Instant? = null,
    val status: DoseStatus
)

enum class DoseStatus {
    TAKEN,
    MISSED,
    SKIPPED
}
