package com.mannat.medicine_reminder.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannat.medicine_reminder.domain.model.AdherenceStats
import com.mannat.medicine_reminder.domain.model.DailyDoseItem
import com.mannat.medicine_reminder.domain.model.DateRange
import com.mannat.medicine_reminder.domain.model.DayAdherence
import com.mannat.medicine_reminder.domain.model.Medicine
import com.mannat.medicine_reminder.domain.repository.DoseLogRepository
import com.mannat.medicine_reminder.domain.usecase.doselog.GetAdherenceStatsUseCase
import com.mannat.medicine_reminder.domain.usecase.doselog.GetDoseLogsForDateUseCase
import com.mannat.medicine_reminder.domain.usecase.medicine.GetAllMedicinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class HistoryUiState(
    val selectedMedicineId: Long? = null,
    val medicines: List<Medicine> = emptyList(),
    val dateRange: DateRange = DateRange.LAST_30_DAYS,
    val stats: AdherenceStats? = null,
    val calendarMonth: YearMonth = YearMonth.now(),
    val calendarDays: List<DayAdherence> = emptyList(),
    val selectedDayDoses: List<DailyDoseItem> = emptyList(),
    val selectedDay: LocalDate? = null,
    val isLoading: Boolean = true
)

sealed class HistoryEvent {
    data class HistoryCleared(val medicineName: String) : HistoryEvent()
}

private data class HistoryFilters(
    val medicineId: Long?,
    val dateRange: DateRange,
    val calendarMonth: YearMonth
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getAllMedicinesUseCase: GetAllMedicinesUseCase,
    private val getAdherenceStatsUseCase: GetAdherenceStatsUseCase,
    private val getDoseLogsForDateUseCase: GetDoseLogsForDateUseCase,
    private val doseLogRepository: DoseLogRepository
) : ViewModel() {

    private val _filters = MutableStateFlow(HistoryFilters(null, DateRange.LAST_30_DAYS, YearMonth.now()))
    private val _dayDetail = MutableStateFlow<Pair<LocalDate?, List<DailyDoseItem>>>(null to emptyList())

    private val _events = MutableSharedFlow<HistoryEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<HistoryUiState> = combine(
        getAllMedicinesUseCase(activeOnly = false),
        _filters,
        _dayDetail
    ) { medicines, filters, dayDetail ->
        Triple(medicines, filters, dayDetail)
    }.flatMapLatest { (medicines, filters, dayDetail) ->
        val endDate = LocalDate.now()
        val startDate = filters.dateRange.startDate()

        val statsFlow = getAdherenceStatsUseCase.invoke(filters.medicineId, startDate, endDate)

        val calStart = filters.calendarMonth.atDay(1)
        val calEnd = filters.calendarMonth.atEndOfMonth()
        val calendarFlow = getAdherenceStatsUseCase.invoke(filters.medicineId, calStart, calEnd)

        combine(statsFlow, calendarFlow) { stats, calStats ->
            HistoryUiState(
                selectedMedicineId = filters.medicineId,
                medicines = medicines,
                dateRange = filters.dateRange,
                stats = stats,
                calendarMonth = filters.calendarMonth,
                calendarDays = calStats.dailyBreakdown,
                selectedDayDoses = dayDetail.second,
                selectedDay = dayDetail.first,
                isLoading = false
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HistoryUiState()
    )

    fun onMedicineSelected(medicineId: Long?) {
        _filters.update { it.copy(medicineId = medicineId) }
        _dayDetail.value = null to emptyList()
    }

    fun onDateRangeSelected(dateRange: DateRange) {
        _filters.update { it.copy(dateRange = dateRange) }
    }

    fun onCalendarMonthChange(month: YearMonth) {
        _filters.update { it.copy(calendarMonth = month) }
    }

    fun onDaySelected(date: LocalDate) {
        viewModelScope.launch {
            val doses = getDoseLogsForDateUseCase(date).first()
            val medicineId = _filters.value.medicineId
            val filtered = if (medicineId != null) {
                doses.filter { it.medicineId == medicineId }
            } else {
                doses
            }
            _dayDetail.value = date to filtered
        }
    }

    fun onDayDismissed() {
        _dayDetail.value = null to emptyList()
    }

    fun clearHistoryForMedicine(medicineId: Long, medicineName: String) {
        viewModelScope.launch {
            doseLogRepository.clearHistoryForMedicine(medicineId)
            _events.emit(HistoryEvent.HistoryCleared(medicineName))
        }
    }
}
