package com.mannat.medicine_reminder.domain.usecase.doselog

import com.mannat.medicine_reminder.domain.repository.DoseLogRepository
import java.time.LocalDate
import javax.inject.Inject

class UndoDoseTakenUseCase @Inject constructor(
    private val repository: DoseLogRepository
) {
    suspend operator fun invoke(scheduleId: Long, date: LocalDate) {
        repository.undoLogDose(scheduleId, date)
    }
}
