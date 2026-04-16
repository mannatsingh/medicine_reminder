package com.mannat.medicine_reminder.domain.model

import java.time.LocalTime

data class Schedule(
    val id: Long = 0,
    val medicineId: Long = 0,
    val time: LocalTime
)
