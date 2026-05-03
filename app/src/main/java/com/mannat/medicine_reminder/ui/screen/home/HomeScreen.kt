package com.mannat.medicine_reminder.ui.screen.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mannat.medicine_reminder.domain.model.DoseStatus
import com.mannat.medicine_reminder.ui.component.CalendarStrip
import com.mannat.medicine_reminder.ui.component.DoseCheckItem
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onEditMedicine: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.DoseToggled -> {
                    val message = when (event.newStatus) {
                        DoseStatus.TAKEN -> "${event.medicineName} marked as taken"
                        DoseStatus.SKIPPED -> "${event.medicineName} skipped"
                        else -> "${event.medicineName} unmarked"
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onUndoDose(event.scheduleId, event.date)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CalendarStrip(
                selectedDate = uiState.selectedDate,
                onDateSelected = viewModel::onDateSelected,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Progress bar — for the selected date (typically today)
            if (!uiState.isLoading && uiState.totalDoses > 0) {
                val animatedProgress by animateFloatAsState(
                    targetValue = uiState.progress,
                    animationSpec = tween(durationMillis = 500),
                    label = "progress"
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Today's Progress",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "${uiState.takenDoses} of ${uiState.totalDoses} taken",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.small),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.sections.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No medicines scheduled",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap + to add a medicine",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.sections.forEachIndexed { index, section ->
                        // Day section header
                        item(key = "header-${section.date}") {
                            Column(modifier = Modifier.padding(top = if (index == 0) 0.dp else 16.dp)) {
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(bottom = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = section.label,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = section.date.format(
                                            DateTimeFormatter.ofPattern("EEE, MMM d")
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Time-of-day groups within each day
                        section.groupedByTime.forEach { (timeOfDay, items) ->
                            item(key = "time-${section.date}-${timeOfDay.name}") {
                                Text(
                                    text = timeOfDay.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                )
                            }
                            items(
                                items = items,
                                key = { "${section.date}-${it.scheduleId}" }
                            ) { doseItem ->
                                DoseCheckItem(
                                    item = doseItem,
                                    selectedDate = section.date,
                                    onToggle = {
                                        viewModel.onToggleDose(
                                            doseItem.scheduleId,
                                            doseItem.medicineName,
                                            section.date,
                                            doseItem.status
                                        )
                                    },
                                    onSkip = {
                                        if (doseItem.status == DoseStatus.SKIPPED) {
                                            viewModel.onUndoDose(doseItem.scheduleId, section.date)
                                        } else {
                                            viewModel.onSkipDose(
                                                doseItem.scheduleId,
                                                doseItem.medicineName,
                                                section.date
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
