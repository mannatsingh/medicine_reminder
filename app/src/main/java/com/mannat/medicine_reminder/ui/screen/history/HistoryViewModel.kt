package com.mannat.medicine_reminder.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannat.medicine_reminder.domain.model.AdherenceStats
import com.mannat.medicine_reminder.domain.model.DateRange
import com.mannat.medicine_reminder.domain.model.Medicine
import com.mannat.medicine_reminder.domain.usecase.doselog.GetAdherenceStatsUseCase
import com.mannat.medicine_reminder.domain.usecase.medicine.GetAllMedicinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class HistoryUiState(
    val selectedMedicineId: Long? = null,
    val medicines: List<Medicine> = emptyList(),
    val dateRange: DateRange = DateRange.LAST_30_DAYS,
    val stats: AdherenceStats? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    getAllMedicinesUseCase: GetAllMedicinesUseCase,
    private val getAdherenceStatsUseCase: GetAdherenceStatsUseCase
) : ViewModel() {

    private val _selectedMedicineId = MutableStateFlow<Long?>(null)
    private val _dateRange = MutableStateFlow(DateRange.LAST_30_DAYS)

    val uiState: StateFlow<HistoryUiState> = combine(
        getAllMedicinesUseCase(activeOnly = false),
        _selectedMedicineId,
        _dateRange
    ) { medicines, medicineId, dateRange ->
        Triple(medicines, medicineId, dateRange)
    }.flatMapLatest { (medicines, medicineId, dateRange) ->
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(dateRange.days.toLong())

        getAdherenceStatsUseCase.invoke(medicineId, startDate, endDate)
            .combine(MutableStateFlow(medicines)) { stats, meds ->
                HistoryUiState(
                    selectedMedicineId = medicineId,
                    medicines = meds,
                    dateRange = dateRange,
                    stats = stats,
                    isLoading = false
                )
            }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HistoryUiState()
    )

    fun onMedicineSelected(medicineId: Long?) {
        _selectedMedicineId.value = medicineId
    }

    fun onDateRangeSelected(dateRange: DateRange) {
        _dateRange.value = dateRange
    }
}
