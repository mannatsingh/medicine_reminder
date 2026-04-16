package com.mannat.medicine_reminder.domain.usecase.medicine

import com.mannat.medicine_reminder.domain.model.Medicine
import com.mannat.medicine_reminder.domain.repository.MedicineRepository
import javax.inject.Inject

class GetMedicineByIdUseCase @Inject constructor(
    private val repository: MedicineRepository
) {
    suspend operator fun invoke(id: Long): Medicine? {
        return repository.getMedicineById(id)
    }
}
