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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class DaySection(
    val date: LocalDate,
    val label: String,
    val items: List<DailyDoseItem>,
    val groupedByTime: Map<TimeOfDay, List<DailyDoseItem>>
)

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val sections: List<DaySection> = emptyList(),
    val isLoading: Boolean = true,
    // Progress is for "today" specifically
    val totalDoses: Int = 0,
    val takenDoses: Int = 0,
    val progress: Float = 0f
)

sealed class HomeEvent {
    data class DoseToggled(
        val scheduleId: Long,
        val medicineName: String,
        val date: LocalDate,
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
            // Always show 3 days centered on the selected date:
            // previous day, selected day, next day. This gracefully handles
            // the midnight rollover so yesterday's doses don't disappear.
            val prev = date.minusDays(1)
            val next = date.plusDays(1)

            combine(
                getDoseLogsForDateUseCase(prev),
                getDoseLogsForDateUseCase(date),
                getDoseLogsForDateUseCase(next)
            ) { prevItems, todayItems, nextItems ->
                val sections = listOf(
                    buildSection(prev, prevItems, isCenter = false, isYesterday = true),
                    buildSection(date, todayItems, isCenter = true, isToday = true),
                    buildSection(next, nextItems, isCenter = false, isTomorrow = true)
                ).filter { it.items.isNotEmpty() }

                val total = todayItems.size
                val taken = todayItems.count { it.status == DoseStatus.TAKEN }

                HomeUiState(
                    selectedDate = date,
                    sections = sections,
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

    private fun buildSection(
        date: LocalDate,
        items: List<DailyDoseItem>,
        isCenter: Boolean,
        isYesterday: Boolean = false,
        isToday: Boolean = false,
        isTomorrow: Boolean = false
    ): DaySection {
        val today = LocalDate.now()
        val label = when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            date == today.plusDays(1) -> "Tomorrow"
            else -> date.dayOfWeek.toString()
                .lowercase().replaceFirstChar { it.uppercase() }
        }
        return DaySection(
            date = date,
            label = label,
            items = items,
            groupedByTime = items.groupBy { TimeOfDay.from(it.scheduledTime) }
        )
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun onToggleDose(
        scheduleId: Long,
        medicineName: String,
        date: LocalDate,
        currentStatus: DoseStatus?
    ) {
        viewModelScope.launch {
            if (currentStatus == DoseStatus.TAKEN) {
                undoDoseTakenUseCase(scheduleId, date)
                _events.emit(HomeEvent.DoseToggled(scheduleId, medicineName, date, null))
            } else {
                logDoseTakenUseCase(scheduleId, date, DoseStatus.TAKEN)
                _events.emit(
                    HomeEvent.DoseToggled(scheduleId, medicineName, date, DoseStatus.TAKEN)
                )
            }
        }
    }

    fun onSkipDose(scheduleId: Long, medicineName: String, date: LocalDate) {
        viewModelScope.launch {
            logDoseTakenUseCase(scheduleId, date, DoseStatus.SKIPPED)
            _events.emit(
                HomeEvent.DoseToggled(scheduleId, medicineName, date, DoseStatus.SKIPPED)
            )
        }
    }

    fun onUndoDose(scheduleId: Long, date: LocalDate) {
        viewModelScope.launch {
            undoDoseTakenUseCase(scheduleId, date)
        }
    }
}
