package com.mannat.medicine_reminder.domain.model

import java.time.LocalDate

data class AdherenceStats(
    val totalScheduledDoses: Int,
    val takenDoses: Int,
    val missedDoses: Int,
    val skippedDoses: Int,
    val adherencePercentage: Float,
    val currentStreak: Int,
    val longestStreak: Int,
    val dailyBreakdown: List<DayAdherence>
)

data class DayAdherence(
    val date: LocalDate,
    val scheduledCount: Int,
    val takenCount: Int,
    val adherencePercentage: Float
)

enum class DateRange(val days: Int, val label: String) {
    LAST_7_DAYS(7, "Week"),
    LAST_30_DAYS(30, "Month"),
    LAST_365_DAYS(365, "Year"),
    ALL_TIME(0, "All Time");

    fun startDate(): LocalDate = when (this) {
        ALL_TIME -> LocalDate.of(2020, 1, 1)
        else -> LocalDate.now().minusDays(days.toLong())
    }
}

data class MedicineDoseDetail(
    val scheduleId: Long,
    val date: LocalDate,
    val scheduledTime: java.time.LocalTime,
    val status: DoseStatus?,
    val takenAt: java.time.Instant?
)
