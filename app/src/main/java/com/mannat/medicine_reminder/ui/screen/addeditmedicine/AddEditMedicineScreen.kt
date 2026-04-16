package com.mannat.medicine_reminder.ui.screen.addeditmedicine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mannat.medicine_reminder.domain.model.DosageUnit
import com.mannat.medicine_reminder.domain.model.Frequency
import com.mannat.medicine_reminder.ui.component.TimePickerDialog
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditMedicineScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditMedicineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddEditMedicineEvent.SaveSuccess -> onNavigateBack()
                is AddEditMedicineEvent.SaveError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditing) "Edit Medicine" else "Add Medicine")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name with autocomplete
            val suggestions by viewModel.nameSuggestions.collectAsStateWithLifecycle()

            ExposedDropdownMenuBox(
                expanded = suggestions.isNotEmpty(),
                onExpandedChange = { }
            ) {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Medicine name") },
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                )
                if (suggestions.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = true,
                        onDismissRequest = { viewModel.onNameSuggestionSelected(uiState.name) }
                    ) {
                        suggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = { viewModel.onNameSuggestionSelected(suggestion) }
                            )
                        }
                    }
                }
            }

            // Dosage — amount + unit picker
            Text("Dosage", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = uiState.dosageAmount,
                    onValueChange = viewModel::onDosageAmountChange,
                    label = { Text("Amount") },
                    isError = uiState.dosageError != null,
                    supportingText = uiState.dosageError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                var unitExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.dosageUnit.abbreviation,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) },
                        singleLine = true,
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        DosageUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.abbreviation} — ${unit.label}") },
                                onClick = {
                                    viewModel.onDosageUnitChange(unit)
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Frequency
            Text("Frequency", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = uiState.frequency == Frequency.DAILY,
                    onClick = { viewModel.onFrequencyChange(Frequency.DAILY) }
                )
                Text("Every day", modifier = Modifier.padding(end = 16.dp))
                RadioButton(
                    selected = uiState.frequency == Frequency.SPECIFIC_DAYS,
                    onClick = { viewModel.onFrequencyChange(Frequency.SPECIFIC_DAYS) }
                )
                Text("Specific days")
            }

            // Day-of-week chips
            if (uiState.frequency == Frequency.SPECIFIC_DAYS) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in uiState.activeDays,
                            onClick = { viewModel.onDayToggle(day) },
                            label = {
                                Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                            }
                        )
                    }
                }
            }

            // Scheduled times
            Text("Scheduled Times", style = MaterialTheme.typography.titleSmall)
            if (uiState.timesError != null) {
                Text(
                    uiState.timesError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.scheduledTimes.forEach { time ->
                    AssistChip(
                        onClick = { },
                        label = { Text(formatTime(time)) },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.onRemoveTime(time) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove time",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    )
                }
                AssistChip(
                    onClick = { showTimePicker = true },
                    label = { Text("Add time") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            HorizontalDivider()

            // Notification settings
            Text("Notifications", style = MaterialTheme.typography.titleSmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable notifications", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Get reminded at scheduled times",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = viewModel::onNotificationsEnabledChange
                )
            }

            // Reminder window
            if (uiState.notificationsEnabled) {
                Text("Follow-up reminder", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Get a second reminder if you haven't taken the dose",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReminderWindowOption.entries.forEach { option ->
                        FilterChip(
                            selected = uiState.reminderWindow == option,
                            onClick = { viewModel.onReminderWindowChange(option) },
                            label = { Text(option.label) }
                        )
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes (optional)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // Save button
            Button(
                onClick = viewModel::onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSaving) "Saving..." else "Save")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                viewModel.onAddTime(
                    LocalTime.of(timePickerState.hour, timePickerState.minute)
                )
                showTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour % 12 == 0) 12 else time.hour % 12
    val amPm = if (time.hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(hour, time.minute, amPm)
}
