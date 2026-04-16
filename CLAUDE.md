# Medicine Reminder — Developer Guide

## Overview

Android medicine reminder app. Users add medicines with dosage (amount + unit picker), frequency, and scheduled times. Each day they check off doses via a home screen checklist with progress tracking. Notifications fire at scheduled times via AlarmManager, with configurable follow-up reminders if doses aren't marked as taken. Data is stored locally in Room with optional Google Drive backup.

## Build

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_HOME=~/Library/Android/sdk
./gradlew assembleDebug
```

Requirements: JDK 17 (Temurin), Android SDK 35, build-tools 35.0.0. The `local.properties` file points to the SDK.

Install on device: `$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk`

## Tech Stack

- **Kotlin** + **Jetpack Compose** (Material Design 3, dynamic color on Android 12+)
- **Hilt** for dependency injection
- **Room** for local SQLite database (currently schema version 2)
- **Navigation Compose** for screen navigation with bottom nav (4 tabs)
- **DataStore** for user preferences (notifications, backup, default reminder window)
- **AlarmManager** for exact-time dose reminders (not WorkManager — timing must be precise)
- **WorkManager** for daily Google Drive backup (deferrable, approximate timing is fine)
- **Google Drive REST API** (`drive.appdata` scope) for cloud backup

## Architecture

Single `:app` module. Clean Architecture enforced via package structure:

```
com.mannat.medicine_reminder/
├── data/
│   ├── local/db/          — Room entities, DAOs, AppDatabase (v2), converters
│   ├── local/datastore/   — PreferencesManager (notifications, backup, reminder window)
│   ├── remote/backup/     — GoogleDriveBackupService
│   ├── repository/        — MedicineRepositoryImpl, DoseLogRepositoryImpl, BackupRepositoryImpl
│   └── mapper/            — Entity ↔ domain model mappers
├── domain/
│   ├── model/             — Medicine (with DosageUnit enum), Schedule, DoseLog, DailyDoseItem, AdherenceStats
│   ├── repository/        — Interfaces (MedicineRepository, DoseLogRepository, BackupRepository)
│   └── usecase/           — medicine/ (CRUD + alarm wiring), doselog/ (logging, adherence), alarm/, backup/
├── ui/
│   ├── MainActivity.kt    — Entry point, notification permission prompt on first launch
│   ├── navigation/        — NavGraph (Scaffold + bottom nav + FAB), Screen sealed class, BottomNavItem
│   ├── theme/             — Color (includes adherence colors), Theme (dynamic color), Type
│   ├── screen/
│   │   ├── home/          — Daily dose checklist with progress bar, snackbar undo, overdue indicators
│   │   ├── medicines/     — Medicine list with search/filter, edit/delete with confirmation
│   │   ├── addeditmedicine/ — Form: name, dosage (amount + unit picker), frequency, times, notification settings, reminder window
│   │   ├── history/       — Month calendar view, stats cards, day detail bottom sheet, clear history
│   │   └── settings/      — Notification toggle, default reminder window, Google Drive backup
│   └── component/         — CalendarStrip, DoseCheckItem, TimePickerDialog, MonthCalendar, AdherenceChart
├── notification/
│   ├── AlarmScheduler.kt         — Schedules exact alarms + follow-up alarms
│   ├── AlarmReceiver.kt          — Fires notification, schedules follow-up, reschedules for next day
│   ├── FollowUpAlarmReceiver.kt  — Checks if dose taken, sends "Don't forget" reminder if not
│   ├── BootReceiver.kt           — Reschedules all alarms after reboot
│   ├── DoseActionReceiver.kt     — Handles "Taken" button from notification
│   └── NotificationHelper.kt     — Notification channel, dose reminders, follow-up reminders
├── backup/                — BackupWorker (WorkManager periodic task)
├── util/                  — FlowUtils (combine for 8+ flows)
└── di/                    — AppModule, DatabaseModule (with migration), RepositoryModule
```

**MVVM pattern**: Each screen has a `*Screen.kt` (Composable) and `*ViewModel.kt`. ViewModels expose `StateFlow<UiState>` and receive user actions as method calls. Use cases sit between ViewModels and repositories.

## Database Schema

Three tables with foreign key relationships. **Current version: 2** (migration adds reminderWindowMinutes and notificationsEnabled to medicines).

**`medicines`** — id (PK), name, dosage, frequency (DAILY|SPECIFIC_DAYS), activeDays (comma-separated day-of-week ints 1-7), notes, isActive, reminderWindowMinutes (nullable, null = use default), notificationsEnabled (boolean), createdAt, updatedAt

**`schedules`** — id (PK), medicineId (FK→medicines, CASCADE), hour (0-23), minute (0-59)
- One row per scheduled time slot. A medicine taken 3x/day has 3 schedule rows.
- The schedule `id` is used as the PendingIntent request code for alarms — must be stable and unique.

**`dose_logs`** — id (PK), scheduleId (FK→schedules, CASCADE), date (ISO string "2026-04-15"), takenAt (epoch millis, nullable), status (TAKEN|MISSED|SKIPPED)
- Unique constraint on (scheduleId, date). A missing row means the dose is pending/future.

### Why this schema?

- Separate `schedules` table (vs JSON array) enables stable foreign keys into `dose_logs` and unique PendingIntent IDs for alarms.
- `date` stored as ISO string (not epoch) avoids timezone ambiguity for date-only comparisons.
- Soft delete on medicines (`isActive = false`) preserves history data. Inactive medicines still appear in History dropdown.

## Key Files

**Most complex logic lives in these files:**

- `domain/usecase/doselog/GetDoseLogsForDateUseCase.kt` — Computes expected doses for a date by cross-referencing active medicines, their activeDays (day-of-week filter), their schedules, and existing dose logs. This is the core daily-tracking logic.
- `domain/usecase/doselog/GetAdherenceStatsUseCase.kt` — Streak calculation and adherence percentage across date ranges. Respects medicine creation dates (doesn't penalize for days before a medicine existed). Defaults to 100% when no data (new user).
- `notification/AlarmScheduler.kt` — Schedules one-shot exact alarms + follow-up alarms. Each alarm self-reschedules after firing (via AlarmReceiver), which allows day-of-week filtering per fire.
- `notification/AlarmReceiver.kt` — BroadcastReceiver that fires notifications, schedules follow-up reminders based on reminder window, and reschedules for next day. Uses `goAsync()` with coroutine for DB access.
- `notification/FollowUpAlarmReceiver.kt` — Fires after the reminder window. Checks if dose was taken; if not, sends a "Don't forget" notification.
- `data/repository/BackupRepositoryImpl.kt` — Closes Room DB, copies the SQLite file, uploads to Drive, reopens. Restore is the reverse + alarm rescheduling.
- `ui/screen/history/HistoryViewModel.kt` — Manages calendar navigation, day selection, stats computation, and history clearing.

## Notification System

Alarms use `AlarmManager.setExactAndAllowWhileIdle()` (works in Doze mode). Flow:

1. User adds medicine → `AddMedicineUseCase` saves to DB and calls `AlarmScheduler.scheduleAlarm()` for each schedule
2. `AlarmScheduler` sets a one-shot alarm with `scheduleId` as the PendingIntent request code
3. `AlarmReceiver.onReceive()` fires → checks if medicine is active, has notifications enabled, and today is an active day → shows notification → schedules follow-up alarm (based on reminder window) → reschedules main alarm for next day
4. Notification has a "Taken" action button → `DoseActionReceiver` logs the dose and dismisses notification
5. If the user doesn't act within the reminder window → `FollowUpAlarmReceiver` checks if dose was logged → if not, sends a "Don't forget" notification
6. `BootReceiver` reschedules all alarms after device reboot
7. `UpdateMedicineUseCase` and `DeleteMedicineUseCase` cancel old alarms and schedule new ones

**Reminder window**: Default is 2 hours (configurable in Settings). Per-medicine override options: use default, 30m, 1h, 2h, 4h, or disabled. Medicines can also have notifications disabled entirely.

**Permissions**: `SCHEDULE_EXACT_ALARM` (API 31+), `POST_NOTIFICATIONS` (API 33+ — prompted on first launch), `RECEIVE_BOOT_COMPLETED`.

## Screens

1. **Home** (`screen/home/`) — CalendarStrip (horizontal date scroller) + animated progress bar ("X of Y taken") + dose checklist grouped by Morning/Afternoon/Evening/Night. Overdue doses highlighted in red. Snackbar with undo on dose toggle. FAB to add medicine.
2. **Medicines** (`screen/medicines/`) — Searchable list of all active medicines. Each card shows name, dosage, schedule times, frequency. Edit and delete buttons with confirmation dialog.
3. **Add/Edit Medicine** (`screen/addeditmedicine/`) — Form with name, dosage (amount field + unit dropdown: mg, g, mcg, IU, mL, drops, tablets, capsules, puffs, patches, tsp), frequency radio buttons, day-of-week chips, time chips with TimePicker, notification toggle, follow-up reminder window selector, notes.
4. **History** (`screen/history/`) — Medicine dropdown (active + inactive with labels), date range chips (Week/Month/Year/All Time), adherence % card, streak cards, taken/missed/skipped breakdown, full month calendar with color-coded days (tap for day detail bottom sheet), clear history with two-step confirmation.
5. **Settings** (`screen/settings/`) — Notification toggle, default follow-up reminder window (30m/1h/2h/4h), Google Drive backup section (sign-in, auto-backup toggle, manual backup/restore, last backup timestamp).

## Google Drive Backup

Uses `drive.appdata` scope (hidden app-specific folder — never touches user's personal files). Backs up the raw Room SQLite file. WorkManager runs daily with network + battery constraints.

**Setup**: Requires a Google Cloud Console project with Drive API enabled and an Android OAuth client ID (package: `com.mannat.medicine_reminder`, debug SHA-1 from `~/.android/debug.keystore`). Add your email as a test user in the OAuth consent screen.

**Restore flow**: Download DB → close Room → replace file → delete WAL/SHM → reschedule all alarms.

## Dosage Units

The `DosageUnit` enum in `domain/model/Medicine.kt` defines supported units: mg, g, mcg, IU, mL, drops, tablets, capsules, puffs, patches, tsp. Dosage is stored as a single string (e.g., "500 mg") with `DosageUnit.parseDosage()` and `formatDosage()` for conversion. Adding a new unit only requires adding an entry to the enum.

## Adding a New Feature Checklist

1. Domain model in `domain/model/` if needed
2. Room entity update in `data/local/db/entity/` + migration in `AppDatabase` if schema changes
3. DAO update in `data/local/db/dao/`
4. Repository interface in `domain/repository/`, implementation in `data/repository/`
5. Use case in `domain/usecase/`
6. Hilt bindings in `di/` modules if new repository
7. ViewModel + Screen in `ui/screen/`
8. Add to `NavGraph.kt` and `BottomNavItem` if it's a new tab
9. Update mappers in `data/mapper/` if entity/domain model changed

## Testing

```bash
./gradlew test          # Unit tests
./gradlew assembleDebug # Build debug APK
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`
