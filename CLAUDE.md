# Medicine Reminder — Developer Guide

## Overview

Android medicine reminder app. Users add medicines with dosage, frequency, and scheduled times. Each day they check off doses via a home screen checklist. Notifications fire at scheduled times via AlarmManager. Data is stored locally in Room with optional Google Drive backup.

## Build

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_HOME=~/Library/Android/sdk
./gradlew assembleDebug
```

Requirements: JDK 17 (Temurin), Android SDK 35, build-tools 35.0.0. The `local.properties` file points to the SDK.

## Tech Stack

- **Kotlin** + **Jetpack Compose** (Material Design 3, dynamic color on Android 12+)
- **Hilt** for dependency injection
- **Room** for local SQLite database
- **Navigation Compose** for screen navigation with bottom nav
- **DataStore** for user preferences
- **AlarmManager** for exact-time dose reminders (not WorkManager — timing must be precise)
- **WorkManager** for daily Google Drive backup (deferrable, approximate timing is fine)
- **Google Drive REST API** (`drive.appdata` scope) for cloud backup

## Architecture

Single `:app` module. Clean Architecture enforced via package structure:

```
com.mannat.medicine_reminder/
├── data/           — Room DB, DataStore, Google Drive service, repository impls, mappers
├── domain/         — Models, repository interfaces, use cases
├── ui/             — Compose screens, ViewModels, theme, navigation, reusable components
├── notification/   — AlarmScheduler, AlarmReceiver, BootReceiver, DoseActionReceiver, NotificationHelper
├── backup/         — BackupWorker (WorkManager periodic task)
└── di/             — Hilt modules (AppModule, DatabaseModule, RepositoryModule)
```

**MVVM pattern**: Each screen has a `*Screen.kt` (Composable) and `*ViewModel.kt`. ViewModels expose `StateFlow<UiState>` and receive user actions as method calls. Use cases sit between ViewModels and repositories.

## Database Schema

Three tables with foreign key relationships:

**`medicines`** — id (PK), name, dosage, frequency (DAILY|SPECIFIC_DAYS), activeDays (comma-separated day-of-week ints 1-7), notes, isActive, createdAt, updatedAt

**`schedules`** — id (PK), medicineId (FK→medicines, CASCADE), hour (0-23), minute (0-59)
- One row per scheduled time slot. A medicine taken 3x/day has 3 schedule rows.
- The schedule `id` is used as the PendingIntent request code for alarms — must be stable and unique.

**`dose_logs`** — id (PK), scheduleId (FK→schedules, CASCADE), date (ISO string "2026-04-15"), takenAt (epoch millis, nullable), status (TAKEN|MISSED|SKIPPED)
- Unique constraint on (scheduleId, date). A missing row means the dose is pending/future.

### Why this schema?

- Separate `schedules` table (vs JSON array) enables stable foreign keys into `dose_logs` and unique PendingIntent IDs for alarms.
- `date` stored as ISO string (not epoch) avoids timezone ambiguity for date-only comparisons.
- Soft delete on medicines (`isActive = false`) preserves history data.

## Key Files

**Most complex logic lives in these files:**

- `domain/usecase/doselog/GetDoseLogsForDateUseCase.kt` — Computes expected doses for a date by cross-referencing active medicines, their activeDays (day-of-week filter), their schedules, and existing dose logs. This is the core daily-tracking logic.
- `notification/AlarmScheduler.kt` — Schedules one-shot exact alarms. Each alarm self-reschedules after firing (via AlarmReceiver), which allows day-of-week filtering per fire.
- `notification/AlarmReceiver.kt` — BroadcastReceiver that fires notifications and reschedules. Uses `goAsync()` with coroutine for DB access.
- `data/repository/BackupRepositoryImpl.kt` — Closes Room DB, copies the SQLite file, uploads to Drive, reopens. Restore is the reverse + alarm rescheduling.
- `domain/usecase/doselog/GetAdherenceStatsUseCase.kt` — Streak calculation and adherence percentage across date ranges.

## Notification System

Alarms use `AlarmManager.setExactAndAllowWhileIdle()` (works in Doze mode). Flow:

1. User adds medicine → `AddMedicineUseCase` saves to DB (alarm scheduling will be integrated here)
2. `AlarmScheduler.scheduleAlarm()` sets a one-shot alarm with `scheduleId` as the PendingIntent request code
3. `AlarmReceiver.onReceive()` fires → checks if medicine is active and today is an active day → shows notification → reschedules for next day
4. Notification has a "Taken" action button → `DoseActionReceiver` logs the dose and dismisses notification
5. `BootReceiver` reschedules all alarms after device reboot

**Permissions**: `SCHEDULE_EXACT_ALARM` (API 31+), `POST_NOTIFICATIONS` (API 33+), `RECEIVE_BOOT_COMPLETED`.

## Screens

1. **Home** (`screen/home/`) — CalendarStrip (horizontal date scroller) + dose checklist grouped by Morning/Afternoon/Evening/Night. FAB to add medicine.
2. **Add/Edit Medicine** (`screen/addeditmedicine/`) — Form with name, dosage, frequency radio buttons, day-of-week chips, time chips with TimePicker, notes. Validates required fields.
3. **History** (`screen/history/`) — Medicine dropdown filter, date range chips (7/30/90 days), adherence %, streak cards, color-coded calendar grid.
4. **Settings** (`screen/settings/`) — Notification toggle, Google Drive sign-in, auto-backup toggle, manual backup/restore.

## Google Drive Backup

Uses `drive.appdata` scope (hidden app-specific folder — never touches user's personal files). Backs up the raw Room SQLite file. WorkManager runs daily with network + battery constraints.

**Restore flow**: Download DB → close Room → replace file → delete WAL/SHM → reschedule all alarms.

## Adding a New Feature Checklist

1. Domain model in `domain/model/` if needed
2. Repository interface in `domain/repository/`, implementation in `data/repository/`
3. Use case in `domain/usecase/`
4. Hilt bindings in `di/` modules if new repository
5. ViewModel + Screen in `ui/screen/`
6. Add to `NavGraph.kt` if it's a new screen

## Testing

```bash
./gradlew test          # Unit tests
./gradlew assembleDebug # Build debug APK
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`
