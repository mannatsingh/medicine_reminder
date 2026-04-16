package com.mannat.medicine_reminder.domain.usecase.medicine

import com.mannat.medicine_reminder.domain.model.Medicine
import com.mannat.medicine_reminder.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllMedicinesUseCase @Inject constructor(
    private val repository: MedicineRepository
) {
    operator fun invoke(activeOnly: Boolean = true): Flow<List<Medicine>> {
        return if (activeOnly) repository.getAllActiveMedicines()
        else repository.getAllMedicines()
    }
}
