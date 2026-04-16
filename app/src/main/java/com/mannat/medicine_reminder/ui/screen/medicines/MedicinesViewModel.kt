package com.mannat.medicine_reminder.ui.screen.medicines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannat.medicine_reminder.domain.model.Medicine
import com.mannat.medicine_reminder.domain.usecase.medicine.DeleteMedicineUseCase
import com.mannat.medicine_reminder.domain.usecase.medicine.GetAllMedicinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicinesUiState(
    val medicines: List<Medicine> = emptyList(),
    val filteredMedicines: List<Medicine> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class MedicinesViewModel @Inject constructor(
    getAllMedicinesUseCase: GetAllMedicinesUseCase,
    private val deleteMedicineUseCase: DeleteMedicineUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<MedicinesUiState> = combine(
        getAllMedicinesUseCase(activeOnly = true),
        _searchQuery
    ) { medicines, query ->
        val filtered = if (query.isBlank()) medicines
        else medicines.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.dosage.contains(query, ignoreCase = true) ||
                    it.notes?.contains(query, ignoreCase = true) == true
        }
        MedicinesUiState(
            medicines = medicines,
            filteredMedicines = filtered,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MedicinesUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onDeleteMedicine(id: Long) {
        viewModelScope.launch {
            deleteMedicineUseCase(id)
        }
    }
}
