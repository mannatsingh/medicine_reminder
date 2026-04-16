package com.mannat.medicine_reminder.ui.screen.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).result
            viewModel.onSignInResult(account?.email)
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        // Notifications
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Notifications", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Receive reminders for your medicines",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = viewModel::onNotificationsToggle
                )
            }
        }

        // Default reminder window
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Default Follow-up Reminder", style = MaterialTheme.typography.titleMedium)
                Text(
                    "If a dose isn't marked as taken within this time, you'll get a second notification. Individual medicines can override this.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val options = listOf(30, 60, 120, 240)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { minutes ->
                        val label = if (minutes < 60) "${minutes}m" else "${minutes / 60}h"
                        FilterChip(
                            selected = uiState.defaultReminderWindowMinutes == minutes,
                            onClick = { viewModel.onDefaultReminderWindowChange(minutes) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // Google Drive Backup
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Google Drive Backup", style = MaterialTheme.typography.titleMedium)

                if (uiState.googleAccountEmail != null) {
                    Text(
                        "Signed in as ${uiState.googleAccountEmail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = viewModel::onSignOut) {
                        Text("Sign out")
                    }

                    HorizontalDivider()

                    // Auto-backup toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daily auto-backup")
                        Switch(
                            checked = uiState.backupEnabled,
                            onCheckedChange = viewModel::onBackupToggle
                        )
                    }

                    // Manual backup/restore
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = viewModel::onBackupNow,
                            enabled = !uiState.isBackingUp && !uiState.isRestoring,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (uiState.isBackingUp) "Backing up..." else "Backup now")
                        }
                        OutlinedButton(
                            onClick = viewModel::onRestore,
                            enabled = !uiState.isBackingUp && !uiState.isRestoring,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (uiState.isRestoring) "Restoring..." else "Restore")
                        }
                    }

                    // Last backup time
                    uiState.lastBackupTime?.let {
                        Text(
                            "Last backup: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        "Sign in to back up your data to Google Drive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { signInLauncher.launch(viewModel.getSignInIntent()) }) {
                        Text("Sign in with Google")
                    }
                }
            }
        }

        SnackbarHost(snackbarHostState)
        Spacer(modifier = Modifier.height(16.dp))
    }
}
