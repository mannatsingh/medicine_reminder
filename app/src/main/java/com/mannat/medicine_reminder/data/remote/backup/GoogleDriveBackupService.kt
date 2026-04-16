package com.mannat.medicine_reminder.data.remote.backup

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveBackupService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val BACKUP_FILE_NAME = "medicine_reminder_backup.db"
        private const val APP_DATA_FOLDER = "appDataFolder"
    }

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("MedCheck")
            .build()
    }

    suspend fun uploadDatabaseFile(dbFile: File): Result<Unit> = runCatching {
        val driveService = getDriveService()
            ?: throw IllegalStateException("Not signed in to Google")

        // Check if backup already exists
        val existingFileId = findBackupFileId(driveService)

        val fileMetadata = com.google.api.services.drive.model.File().apply {
            name = BACKUP_FILE_NAME
            if (existingFileId == null) {
                parents = listOf(APP_DATA_FOLDER)
            }
        }
        val mediaContent = FileContent("application/octet-stream", dbFile)

        if (existingFileId != null) {
            driveService.files().update(existingFileId, fileMetadata, mediaContent).execute()
        } else {
            driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
        }
    }

    suspend fun downloadDatabaseFile(destinationFile: File): Result<Unit> = runCatching {
        val driveService = getDriveService()
            ?: throw IllegalStateException("Not signed in to Google")

        val fileId = findBackupFileId(driveService)
            ?: throw IllegalStateException("No backup found")

        FileOutputStream(destinationFile).use { outputStream ->
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        }
    }

    suspend fun hasBackup(): Boolean {
        val driveService = getDriveService() ?: return false
        return findBackupFileId(driveService) != null
    }

    private fun findBackupFileId(driveService: Drive): String? {
        val result = driveService.files().list()
            .setSpaces(APP_DATA_FOLDER)
            .setQ("name = '$BACKUP_FILE_NAME'")
            .setFields("files(id)")
            .setPageSize(1)
            .execute()
        return result.files?.firstOrNull()?.id
    }
}
