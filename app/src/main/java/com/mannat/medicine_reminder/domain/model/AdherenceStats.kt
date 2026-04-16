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
    LAST_7_DAYS(7, "7 days"),
    LAST_30_DAYS(30, "30 days"),
    LAST_90_DAYS(90, "90 days")
}
