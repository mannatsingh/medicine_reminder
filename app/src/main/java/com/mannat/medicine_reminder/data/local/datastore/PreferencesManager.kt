package com.mannat.medicine_reminder.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BACKUP_ENABLED = booleanPreferencesKey("backup_enabled")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
        val DEFAULT_REMINDER_WINDOW = intPreferencesKey("default_reminder_window_minutes")
        const val DEFAULT_REMINDER_WINDOW_MINUTES = 120
    }

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[NOTIFICATIONS_ENABLED] ?: true
    }

    val backupEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[BACKUP_ENABLED] ?: false
    }

    val lastBackupTimestamp: Flow<Long> = dataStore.data.map { prefs ->
        prefs[LAST_BACKUP_TIMESTAMP] ?: 0L
    }

    val googleAccountEmail: Flow<String?> = dataStore.data.map { prefs ->
        prefs[GOOGLE_ACCOUNT_EMAIL]
    }

    val defaultReminderWindowMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[DEFAULT_REMINDER_WINDOW] ?: DEFAULT_REMINDER_WINDOW_MINUTES
    }

    suspend fun setDefaultReminderWindowMinutes(minutes: Int) {
        dataStore.edit { it[DEFAULT_REMINDER_WINDOW] = minutes }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[BACKUP_ENABLED] = enabled }
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        dataStore.edit { it[LAST_BACKUP_TIMESTAMP] = timestamp }
    }

    suspend fun setGoogleAccountEmail(email: String?) {
        dataStore.edit {
            if (email != null) {
                it[GOOGLE_ACCOUNT_EMAIL] = email
            } else {
                it.remove(GOOGLE_ACCOUNT_EMAIL)
            }
        }
    }
}
