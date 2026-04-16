package com.mannat.medicine_reminder.ui.screen.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mannat.medicine_reminder.domain.model.DateRange
import com.mannat.medicine_reminder.ui.component.AdherenceChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("History", style = MaterialTheme.typography.headlineMedium)

        // Medicine selector
        var expanded by remember { mutableStateOf(false) }
        val selectedName = uiState.medicines
            .find { it.id == uiState.selectedMedicineId }?.name ?: "All medicines"

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedName,
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
                uiState.medicines.forEach { medicine ->
                    DropdownMenuItem(
                        text = { Text(medicine.name) },
                        onClick = {
                            viewModel.onMedicineSelected(medicine.id)
                            expanded = false
                        }
                    )
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

        // Stats cards
        uiState.stats?.let { stats ->
            // Adherence percentage
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "%.0f%%".format(stats.adherencePercentage),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Adherence Rate", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Streak cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${stats.currentStreak}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text("Current Streak", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${stats.longestStreak}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text("Longest Streak", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Dose breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${stats.takenDoses}", style = MaterialTheme.typography.titleLarge)
                    Text("Taken", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${stats.missedDoses}", style = MaterialTheme.typography.titleLarge)
                    Text("Missed", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${stats.skippedDoses}", style = MaterialTheme.typography.titleLarge)
                    Text("Skipped", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Adherence calendar grid
            Text("Daily Overview", style = MaterialTheme.typography.titleSmall)
            AdherenceChart(dailyBreakdown = stats.dailyBreakdown)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
