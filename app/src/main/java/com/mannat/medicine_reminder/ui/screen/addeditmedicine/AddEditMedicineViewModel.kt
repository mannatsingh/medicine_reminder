package com.mannat.medicine_reminder.ui.screen.addeditmedicine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mannat.medicine_reminder.domain.model.DosageUnit
import com.mannat.medicine_reminder.domain.model.Frequency
import com.mannat.medicine_reminder.domain.model.Medicine
import com.mannat.medicine_reminder.domain.model.Schedule
import com.mannat.medicine_reminder.data.local.MedicineNameProvider
import com.mannat.medicine_reminder.domain.usecase.medicine.AddMedicineUseCase
import com.mannat.medicine_reminder.domain.usecase.medicine.GetMedicineByIdUseCase
import com.mannat.medicine_reminder.domain.usecase.medicine.UpdateMedicineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject

enum class ReminderWindowOption(val minutes: Int?, val label: String) {
    USE_DEFAULT(null, "Use default"),
    MIN_30(30, "30 minutes"),
    HOUR_1(60, "1 hour"),
    HOUR_2(120, "2 hours"),
    HOUR_4(240, "4 hours"),
    DISABLED(-1, "No follow-up reminder");

    companion object {
        fun fromMinutes(minutes: Int?): ReminderWindowOption {
            if (minutes == null) return USE_DEFAULT
            return entries.find { it.minutes == minutes } ?: USE_DEFAULT
        }
    }
}

data class AddEditMedicineUiState(
    val name: String = "",
    val dosageAmount: String = "",
    val dosageUnit: DosageUnit = DosageUnit.MG,
    val frequency: Frequency = Frequency.DAILY,
    val activeDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val scheduledTimes: List<LocalTime> = emptyList(),
    val notes: String = "",
    val notificationsEnabled: Boolean = true,
    val reminderWindow: ReminderWindowOption = ReminderWindowOption.USE_DEFAULT,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val nameError: String? = null,
    val dosageError: String? = null,
    val timesError: String? = null
)

sealed class AddEditMedicineEvent {
    data object SaveSuccess : AddEditMedicineEvent()
    data class SaveError(val message: String) : AddEditMedicineEvent()
}

@HiltViewModel
class AddEditMedicineViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addMedicineUseCase: AddMedicineUseCase,
    private val updateMedicineUseCase: UpdateMedicineUseCase,
    private val getMedicineByIdUseCase: GetMedicineByIdUseCase,
    private val medicineNameProvider: MedicineNameProvider
) : ViewModel() {

    private val medicineId: Long? = savedStateHandle.get<Long>("medicineId")

    private val _uiState = MutableStateFlow(AddEditMedicineUiState())
    val uiState: StateFlow<AddEditMedicineUiState> = _uiState.asStateFlow()

    private val _nameSuggestions = MutableStateFlow<List<String>>(emptyList())
    val nameSuggestions: StateFlow<List<String>> = _nameSuggestions.asStateFlow()

    private val _events = MutableSharedFlow<AddEditMedicineEvent>()
    val events = _events.asSharedFlow()

    private var existingMedicine: Medicine? = null

    init {
        if (medicineId != null && medicineId != -1L) {
            loadMedicine(medicineId)
        }
    }

    private fun loadMedicine(id: Long) {
        viewModelScope.launch {
            val medicine = getMedicineByIdUseCase(id) ?: return@launch
            existingMedicine = medicine
            val (amount, unit) = DosageUnit.parseDosage(medicine.dosage)
            _uiState.update {
                it.copy(
                    name = medicine.name,
                    dosageAmount = amount,
                    dosageUnit = unit ?: DosageUnit.MG,
                    frequency = medicine.frequency,
                    activeDays = medicine.activeDays,
                    scheduledTimes = medicine.schedules.map { s -> s.time }.sorted(),
                    notes = medicine.notes ?: "",
                    notificationsEnabled = medicine.notificationsEnabled,
                    reminderWindow = ReminderWindowOption.fromMinutes(medicine.reminderWindowMinutes),
                    isEditing = true
                )
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
        viewModelScope.launch {
            _nameSuggestions.value = medicineNameProvider.search(name)
        }
    }

    fun onNameSuggestionSelected(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
        _nameSuggestions.value = emptyList()
    }

    fun onDosageAmountChange(amount: String) {
        _uiState.update { it.copy(dosageAmount = amount, dosageError = null) }
    }

    fun onDosageUnitChange(unit: DosageUnit) {
        _uiState.update { it.copy(dosageUnit = unit) }
    }

    fun onFrequencyChange(frequency: Frequency) {
        _uiState.update {
            it.copy(
                frequency = frequency,
                activeDays = if (frequency == Frequency.DAILY) DayOfWeek.entries.toSet()
                else it.activeDays
            )
        }
    }

    fun onDayToggle(day: DayOfWeek) {
        _uiState.update {
            val newDays = if (day in it.activeDays) it.activeDays - day else it.activeDays + day
            it.copy(activeDays = newDays)
        }
    }

    fun onAddTime(time: LocalTime) {
        _uiState.update {
            if (time in it.scheduledTimes) return@update it
            it.copy(
                scheduledTimes = (it.scheduledTimes + time).sorted(),
                timesError = null
            )
        }
    }

    fun onRemoveTime(time: LocalTime) {
        _uiState.update {
            it.copy(scheduledTimes = it.scheduledTimes - time)
        }
    }

    fun onNotesChange(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun onNotificationsEnabledChange(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun onReminderWindowChange(option: ReminderWindowOption) {
        _uiState.update { it.copy(reminderWindow = option) }
    }

    fun onSave() {
        val state = _uiState.value

        val nameError = if (state.name.isBlank()) "Name is required" else null
        val dosageError = if (state.dosageAmount.isBlank()) "Dosage is required" else null
        val timesError = if (state.scheduledTimes.isEmpty()) "Add at least one time" else null

        if (nameError != null || dosageError != null || timesError != null) {
            _uiState.update {
                it.copy(nameError = nameError, dosageError = dosageError, timesError = timesError)
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val dosage = DosageUnit.formatDosage(state.dosageAmount, state.dosageUnit)
                val reminderMinutes = when (state.reminderWindow) {
                    ReminderWindowOption.USE_DEFAULT -> null
                    ReminderWindowOption.DISABLED -> -1
                    else -> state.reminderWindow.minutes
                }

                val medicine = Medicine(
                    id = existingMedicine?.id ?: 0,
                    name = state.name.trim(),
                    dosage = dosage,
                    frequency = state.frequency,
                    activeDays = state.activeDays,
                    schedules = state.scheduledTimes.map { time ->
                        Schedule(time = time)
                    },
                    notes = state.notes.takeIf { it.isNotBlank() },
                    isActive = existingMedicine?.isActive ?: true,
                    reminderWindowMinutes = reminderMinutes,
                    notificationsEnabled = state.notificationsEnabled,
                    createdAt = existingMedicine?.createdAt ?: Instant.now(),
                    updatedAt = Instant.now()
                )

                if (state.isEditing) {
                    updateMedicineUseCase(medicine)
                } else {
                    addMedicineUseCase(medicine)
                }

                _events.emit(AddEditMedicineEvent.SaveSuccess)
            } catch (e: Exception) {
                _events.emit(AddEditMedicineEvent.SaveError(e.message ?: "Failed to save"))
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
