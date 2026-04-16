package com.mannat.medicine_reminder.domain.repository

interface BackupRepository {
    suspend fun backupToGoogleDrive(): Result<Unit>
    suspend fun restoreFromGoogleDrive(): Result<Unit>
    suspend fun isBackupAvailable(): Boolean
}
