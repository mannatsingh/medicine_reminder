package com.mannat.medicine_reminder.ui.screen.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mannat.medicine_reminder.domain.model.DateRange
import com.mannat.medicine_reminder.domain.model.DoseStatus
import com.mannat.medicine_reminder.ui.component.MonthCalendar
import com.mannat.medicine_reminder.ui.theme.AdherenceFull
import com.mannat.medicine_reminder.ui.theme.AdherenceNone
import com.mannat.medicine_reminder.ui.theme.AdherencePartial
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Clear history dialog state: null = hidden, 1 = first confirm, 2 = type name to confirm
    var clearDialogStep by remember { mutableStateOf(0) }
    var clearTargetMedicine by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var typedConfirmation by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HistoryEvent.HistoryCleared -> {
                    snackbarHostState.showSnackbar("History cleared for ${event.medicineName}")
                }
            }
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("History", style = MaterialTheme.typography.headlineMedium)

            // Medicine selector — shows inactive medicines with label
            var expanded by remember { mutableStateOf(false) }
            val selectedMedicine = uiState.medicines.find { it.id == uiState.selectedMedicineId }
            val selectedLabel = when {
                selectedMedicine == null -> "All medicines"
                !selectedMedicine.isActive -> "${selectedMedicine.name} (inactive)"
                else -> selectedMedicine.name
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("All medicines") },
                        onClick = {
                            viewModel.onMedicineSelected(null)
                            expanded = false
                        }
                    )
                    // Active medicines first
                    uiState.medicines.filter { it.isActive }.forEach { medicine ->
                        DropdownMenuItem(
                            text = { Text(medicine.name) },
                            onClick = {
                                viewModel.onMedicineSelected(medicine.id)
                                expanded = false
                            }
                        )
                    }
                    // Inactive medicines with label
                    val inactiveMedicines = uiState.medicines.filter { !it.isActive }
                    if (inactiveMedicines.isNotEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "— Previously taken —",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {},
                            enabled = false
                        )
                        inactiveMedicines.forEach { medicine ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${medicine.name} (inactive)",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    viewModel.onMedicineSelected(medicine.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Date range chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateRange.entries.forEach { range ->
                    FilterChip(
                        selected = uiState.dateRange == range,
                        onClick = { viewModel.onDateRangeSelected(range) },
                        label = { Text(range.label) }
                    )
                }
            }

            // Stats summary
            uiState.stats?.let { stats ->
                // Adherence percentage
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "%.0f%%".format(stats.adherencePercentage),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                stats.adherencePercentage >= 80f -> AdherenceFull
                                stats.adherencePercentage >= 50f -> AdherencePartial
                                else -> AdherenceNone
                            }
                        )
                        Text(
                            "Adherence Rate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Streaks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${stats.currentStreak}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Day streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Card(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${stats.longestStreak}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Day streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Best", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Dose breakdown
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(value = stats.takenDoses, label = "Taken", color = AdherenceFull)
                        StatItem(value = stats.missedDoses, label = "Missed", color = AdherenceNone)
                        StatItem(value = stats.skippedDoses, label = "Skipped", color = AdherencePartial)
                        StatItem(value = stats.totalScheduledDoses, label = "Total", color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Calendar
                Text("Calendar", style = MaterialTheme.typography.titleMedium)
                Card(modifier = Modifier.fillMaxWidth()) {
                    MonthCalendar(
                        yearMonth = uiState.calendarMonth,
                        dayData = uiState.calendarDays,
                        selectedDay = uiState.selectedDay,
                        onDayClick = viewModel::onDaySelected,
                        onPreviousMonth = {
                            viewModel.onCalendarMonthChange(uiState.calendarMonth.minusMonths(1))
                        },
                        onNextMonth = {
                            viewModel.onCalendarMonthChange(uiState.calendarMonth.plusMonths(1))
                        },
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(color = AdherenceFull, label = "100%")
                    LegendItem(color = AdherencePartial, label = "Partial")
                    LegendItem(color = AdherenceNone, label = "Missed")
                }

                // Clear history button — only when a specific medicine is selected
                if (uiState.selectedMedicineId != null && selectedMedicine != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    OutlinedButton(
                        onClick = {
                            clearTargetMedicine = selectedMedicine.id to selectedMedicine.name
                            clearDialogStep = 1
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            "Clear history for ${selectedMedicine.name}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Day detail bottom sheet
    if (uiState.selectedDay != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = viewModel::onDayDismissed,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    uiState.selectedDay!!.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.selectedDayDoses.isEmpty()) {
                    Text(
                        "No doses scheduled for this day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    uiState.selectedDayDoses.forEach { dose ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = when (dose.status) {
                                    DoseStatus.TAKEN -> AdherenceFull.copy(alpha = 0.1f)
                                    DoseStatus.SKIPPED -> AdherencePartial.copy(alpha = 0.1f)
                                    DoseStatus.MISSED -> AdherenceNone.copy(alpha = 0.1f)
                                    null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(dose.medicineName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${dose.dosage} at ${formatTime(dose.scheduledTime)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    when (dose.status) {
                                        DoseStatus.TAKEN -> "Taken"
                                        DoseStatus.SKIPPED -> "Skipped"
                                        DoseStatus.MISSED -> "Missed"
                                        null -> "Pending"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = when (dose.status) {
                                        DoseStatus.TAKEN -> AdherenceFull
                                        DoseStatus.SKIPPED -> AdherencePartial
                                        DoseStatus.MISSED -> AdherenceNone
                                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Step 1: "Are you sure?" dialog
    if (clearDialogStep == 1 && clearTargetMedicine != null) {
        AlertDialog(
            onDismissRequest = {
                clearDialogStep = 0
                clearTargetMedicine = null
            },
            title = { Text("Clear history?") },
            text = {
                Text("This will permanently delete all dose records for ${clearTargetMedicine!!.second}. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    clearDialogStep = 2
                    typedConfirmation = ""
                }) {
                    Text("Continue", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clearDialogStep = 0
                    clearTargetMedicine = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Step 2: Type "yes" to confirm
    if (clearDialogStep == 2 && clearTargetMedicine != null) {
        val (medId, medName) = clearTargetMedicine!!
        AlertDialog(
            onDismissRequest = {
                clearDialogStep = 0
                clearTargetMedicine = null
            },
            title = { Text("Confirm deletion") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Type \"yes\" to permanently delete all history for $medName.")
                    OutlinedTextField(
                        value = typedConfirmation,
                        onValueChange = { typedConfirmation = it },
                        label = { Text("Type yes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistoryForMedicine(medId, medName)
                        clearDialogStep = 0
                        clearTargetMedicine = null
                        typedConfirmation = ""
                    },
                    enabled = typedConfirmation.trim().equals("yes", ignoreCase = true)
                ) {
                    Text("Delete permanently", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clearDialogStep = 0
                    clearTargetMedicine = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatItem(
    value: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$value",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegendItem(
    color: androidx.compose.ui.graphics.Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatTime(time: LocalTime): String {
    val hour = if (time.hour % 12 == 0) 12 else time.hour % 12
    val amPm = if (time.hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(hour, time.minute, amPm)
}
