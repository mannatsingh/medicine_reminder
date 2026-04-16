package com.mannat.medicine_reminder.ui.screen.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.services.drive.DriveScopes
import com.mannat.medicine_reminder.backup.BackupWorker
import com.mannat.medicine_reminder.data.local.datastore.PreferencesManager
import com.mannat.medicine_reminder.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val defaultReminderWindowMinutes: Int = 120,
    val backupEnabled: Boolean = false,
    val googleAccountEmail: String? = null,
    val lastBackupTime: String? = null,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val preferencesManager: PreferencesManager,
    private val backupRepository: BackupRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _isBackingUp = MutableStateFlow(false)
    private val _isRestoring = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = com.mannat.medicine_reminder.util.combine(
        preferencesManager.notificationsEnabled,
        preferencesManager.defaultReminderWindowMinutes,
        preferencesManager.backupEnabled,
        preferencesManager.googleAccountEmail,
        preferencesManager.lastBackupTimestamp,
        _isBackingUp,
        _isRestoring,
        _message
    ) { notifEnabled, reminderWindow, backupEnabled, email, lastBackup, backing, restoring, msg ->
        SettingsUiState(
            notificationsEnabled = notifEnabled,
            defaultReminderWindowMinutes = reminderWindow,
            backupEnabled = backupEnabled,
            googleAccountEmail = email,
            lastBackupTime = formatTimestamp(lastBackup),
            isBackingUp = backing,
            isRestoring = restoring,
            message = msg
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState()
    )

    fun onNotificationsToggle(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNotificationsEnabled(enabled)
        }
    }

    fun onDefaultReminderWindowChange(minutes: Int) {
        viewModelScope.launch {
            preferencesManager.setDefaultReminderWindowMinutes(minutes)
        }
    }

    fun onBackupToggle(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setBackupEnabled(enabled)
            if (enabled) {
                BackupWorker.scheduleDailyBackup(workManager)
            } else {
                BackupWorker.cancelDailyBackup(workManager)
            }
        }
    }

    fun getSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(appContext, gso).signInIntent
    }

    fun onSignInResult(email: String?) {
        viewModelScope.launch {
            preferencesManager.setGoogleAccountEmail(email)
        }
    }

    fun onSignOut() {
        viewModelScope.launch {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(appContext, gso).signOut()
            preferencesManager.setGoogleAccountEmail(null)
            preferencesManager.setBackupEnabled(false)
            BackupWorker.cancelDailyBackup(workManager)
        }
    }

    fun onBackupNow() {
        viewModelScope.launch {
            _isBackingUp.value = true
            val result = backupRepository.backupToGoogleDrive()
            _isBackingUp.value = false
            _message.value = if (result.isSuccess) "Backup complete" else "Backup failed: ${result.exceptionOrNull()?.message}"
        }
    }

    fun onRestore() {
        viewModelScope.launch {
            _isRestoring.value = true
            val result = backupRepository.restoreFromGoogleDrive()
            _isRestoring.value = false
            _message.value = if (result.isSuccess) "Restore complete — restart app to see changes" else "Restore failed: ${result.exceptionOrNull()?.message}"
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun formatTimestamp(timestamp: Long): String? {
        if (timestamp == 0L) return null
        val instant = Instant.ofEpochMilli(timestamp)
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }
}
