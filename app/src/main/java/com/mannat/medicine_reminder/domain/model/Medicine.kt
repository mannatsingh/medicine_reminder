package com.mannat.medicine_reminder.domain.model

import java.time.DayOfWeek
import java.time.Instant

data class Medicine(
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val frequency: Frequency,
    val activeDays: Set<DayOfWeek>,
    val schedules: List<Schedule> = emptyList(),
    val notes: String? = null,
    val isActive: Boolean = true,
    val reminderWindowMinutes: Int? = null,
    val notificationsEnabled: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

enum class Frequency {
    DAILY,
    SPECIFIC_DAYS
}

enum class DosageUnit(val label: String, val abbreviation: String) {
    MG("Milligrams", "mg"),
    G("Grams", "g"),
    MCG("Micrograms", "mcg"),
    IU("International Units", "IU"),
    ML("Milliliters", "mL"),
    DROPS("Drops", "drops"),
    TABLETS("Tablets", "tablet(s)"),
    CAPSULES("Capsules", "capsule(s)"),
    PUFFS("Puffs", "puff(s)"),
    PATCHES("Patches", "patch(es)"),
    TEASPOONS("Teaspoons", "tsp");

    companion object {
        fun fromAbbreviation(abbr: String): DosageUnit? {
            return entries.find { it.abbreviation.equals(abbr, ignoreCase = true) }
        }

        fun parseDosage(dosage: String): Pair<String, DosageUnit?> {
            val trimmed = dosage.trim()
            for (unit in entries) {
                if (trimmed.endsWith(unit.abbreviation, ignoreCase = true)) {
                    val amount = trimmed.dropLast(unit.abbreviation.length).trim()
                    return amount to unit
                }
            }
            return trimmed to null
        }

        fun formatDosage(amount: String, unit: DosageUnit): String {
            return "${amount.trim()} ${unit.abbreviation}"
        }
    }
}
