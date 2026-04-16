package com.mannat.medicine_reminder.data.repository

import android.content.Context
import com.mannat.medicine_reminder.data.local.db.AppDatabase
import com.mannat.medicine_reminder.data.remote.backup.GoogleDriveBackupService
import com.mannat.medicine_reminder.data.local.datastore.PreferencesManager
import com.mannat.medicine_reminder.domain.repository.BackupRepository
import com.mannat.medicine_reminder.domain.usecase.alarm.RescheduleAllAlarmsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val driveService: GoogleDriveBackupService,
    private val preferencesManager: PreferencesManager,
    private val rescheduleAllAlarmsUseCase: RescheduleAllAlarmsUseCase
) : BackupRepository {

    override suspend fun backupToGoogleDrive(): Result<Unit> = runCatching {
        // Checkpoint WAL to ensure all data is in the main DB file
        database.query("PRAGMA wal_checkpoint(FULL)", null).close()

        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val tempFile = File(context.cacheDir, "backup_temp.db")

        dbFile.copyTo(tempFile, overwrite = true)

        driveService.uploadDatabaseFile(tempFile).getOrThrow()

        tempFile.delete()

        preferencesManager.setLastBackupTimestamp(System.currentTimeMillis())
    }

    override suspend fun restoreFromGoogleDrive(): Result<Unit> = runCatching {
        val tempFile = File(context.cacheDir, "restore_temp.db")

        driveService.downloadDatabaseFile(tempFile).getOrThrow()

        // Close the database before replacing the file
        database.close()

        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        tempFile.copyTo(dbFile, overwrite = true)
        tempFile.delete()

        // Also copy WAL and SHM cleanup
        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")
        walFile.delete()
        shmFile.delete()

        // Reschedule alarms with restored data
        rescheduleAllAlarmsUseCase()
    }

    override suspend fun isBackupAvailable(): Boolean {
        return driveService.hasBackup()
    }
}
