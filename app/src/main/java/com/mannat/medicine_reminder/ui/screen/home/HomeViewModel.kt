package com.mannat.medicine_reminder.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannat.medicine_reminder.domain.model.DailyDoseItem
import com.mannat.medicine_reminder.domain.model.DoseStatus
import com.mannat.medicine_reminder.domain.usecase.doselog.GetDoseLogsForDateUseCase
import com.mannat.medicine_reminder.domain.usecase.doselog.LogDoseTakenUseCase
import com.mannat.medicine_reminder.domain.usecase.doselog.UndoDoseTakenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val doseItems: List<DailyDoseItem> = emptyList(),
    val groupedItems: Map<TimeOfDay, List<DailyDoseItem>> = emptyMap(),
    val isLoading: Boolean = true,
    val totalDoses: Int = 0,
    val takenDoses: Int = 0,
    val progress: Float = 0f
)

sealed class HomeEvent {
    data class DoseToggled(
        val scheduleId: Long,
        val medicineName: String,
        val newStatus: DoseStatus?
    ) : HomeEvent()
}

enum class TimeOfDay(val label: String) {
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night");

    companion object {
        fun from(time: LocalTime): TimeOfDay = when (time.hour) {
            in 5..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..20 -> EVENING
            else -> NIGHT
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDoseLogsForDateUseCase: GetDoseLogsForDateUseCase,
    private val logDoseTakenUseCase: LogDoseTakenUseCase,
    private val undoDoseTakenUseCase: UndoDoseTakenUseCase
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    private val _events = MutableSharedFlow<HomeEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<HomeUiState> = _selectedDate
        .flatMapLatest { date ->
            getDoseLogsForDateUseCase(date).map { items ->
                val total = items.size
                val taken = items.count { it.status == DoseStatus.TAKEN }
                HomeUiState(
                    selectedDate = date,
                    doseItems = items,
                    groupedItems = items.groupBy { TimeOfDay.from(it.scheduledTime) },
                    isLoading = false,
                    totalDoses = total,
                    takenDoses = taken,
                    progress = if (total > 0) taken.toFloat() / total else 0f
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HomeUiState()
        )

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun onToggleDose(scheduleId: Long, medicineName: String, currentStatus: DoseStatus?) {
        val date = _selectedDate.value
        viewModelScope.launch {
            if (currentStatus == DoseStatus.TAKEN) {
                undoDoseTakenUseCase(scheduleId, date)
                _events.emit(HomeEvent.DoseToggled(scheduleId, medicineName, null))
            } else {
                logDoseTakenUseCase(scheduleId, date, DoseStatus.TAKEN)
                _events.emit(HomeEvent.DoseToggled(scheduleId, medicineName, DoseStatus.TAKEN))
            }
        }
    }

    fun onSkipDose(scheduleId: Long, medicineName: String) {
        val date = _selectedDate.value
        viewModelScope.launch {
            logDoseTakenUseCase(scheduleId, date, DoseStatus.SKIPPED)
            _events.emit(HomeEvent.DoseToggled(scheduleId, medicineName, DoseStatus.SKIPPED))
        }
    }

    fun onUndoDose(scheduleId: Long) {
        val date = _selectedDate.value
        viewModelScope.launch {
            undoDoseTakenUseCase(scheduleId, date)
        }
    }
}
