# MedCheck — Developer Guide

## Overview

Android medicine reminder app, branded **MedCheck**. Users add medicines with dosage (amount + unit picker), frequency, and scheduled times. The Home screen shows a rolling **yesterday + today + tomorrow** view of doses to gracefully handle the midnight rollover. Notifications fire at scheduled times via AlarmManager, with configurable follow-up reminders if doses aren't marked as taken. Data is stored locally in Room with optional Google Drive backup.

## Build

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_HOME=~/Library/Android/sdk
./gradlew assembleDebug
```

Requirements: JDK 17 (Temurin), Android SDK 35, build-tools 35.0.0. The `local.properties` file points to the SDK.

Install on device: `$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk`

Launch: `$ANDROID_HOME/platform-tools/adb shell am start -n com.mannat.medicine_reminder/.ui.MainActivity`

## Tech Stack

- **Kotlin** + **Jetpack Compose** (Material Design 3, dynamic color on Android 12+)
- **Hilt** for dependency injection
- **Room** for local SQLite database (current schema version: **2**)
- **Navigation Compose** for screen navigation with bottom nav (4 tabs: Home, Medicines, History, Settings)
- **DataStore** for user preferences (notifications, backup, default reminder window)
- **AlarmManager** for exact-time dose reminders (NOT WorkManager — timing must be precise)
- **WorkManager** for daily Google Drive backup (deferrable, approximate timing is fine)
- **Google Drive REST API** (`drive.appdata` scope) for cloud backup
- **Bundled drug name database**: 13,827 generic drug names from RxNorm + WHO + supplements (`app/src/main/assets/medicines.json`, ~345KB)

The application id is `com.mannat.medicine_reminder` but the app's display name is **MedCheck** (`res/values/strings.xml`).

## Architecture

Single `:app` module. Clean Architecture enforced via package structure:

```
com.mannat.medicine_reminder/
├── data/
│   ├── local/
│   │   ├── db/                    — Room entities, DAOs, AppDatabase (v2 + MIGRATION_1_2)
│   │   ├── datastore/             — PreferencesManager (notifications, backup, default reminder window)
│   │   └── MedicineNameProvider.kt — Loads + searches the bundled medicines.json (13,827 names)
│   ├── remote/backup/             — GoogleDriveBackupService
│   ├── repository/                — MedicineRepositoryImpl, DoseLogRepositoryImpl, BackupRepositoryImpl
│   └── mapper/                    — Entity ↔ domain model mappers
├── domain/
│   ├── model/                     — Medicine (with DosageUnit enum), Schedule, DoseLog, DailyDoseItem, AdherenceStats, DateRange
│   ├── repository/                — Interfaces (MedicineRepository, DoseLogRepository, BackupRepository)
│   └── usecase/                   — medicine/ (CRUD + alarm wiring), doselog/ (logging, adherence), alarm/, backup/
├── ui/
│   ├── MainActivity.kt            — Entry point. Requests POST_NOTIFICATIONS (API 33+) and SCHEDULE_EXACT_ALARM (API 31+) permissions
│   ├── navigation/                — NavGraph (Scaffold + bottom nav + FAB), Screen sealed class, BottomNavItem
│   ├── theme/                     — Color (incl. AdherenceFull/Partial/None), Theme (dynamic color), Type
│   ├── screen/
│   │   ├── home/                  — Multi-day (yesterday/today/tomorrow) dose checklist, progress bar, snackbar undo, overdue indicators
│   │   ├── medicines/             — Searchable medicine list, edit/delete (two-step "type yes" confirmation)
│   │   ├── addeditmedicine/       — Form: name (autocomplete), dosage (amount + unit), frequency, times, notification settings
│   │   ├── history/               — Multi-range stats (Week/Month/Year/All-Time), month calendar, day detail sheet, clear-history flow
│   │   └── settings/              — Notification toggle, default reminder window, Google Drive backup
│   └── component/                 — CalendarStrip, DoseCheckItem, TimePickerDialog, MonthCalendar, AdherenceChart
├── notification/
│   ├── AlarmScheduler.kt          — Schedules exact alarms + follow-up alarms (cancelFollowUpAlarm is public)
│   ├── AlarmReceiver.kt           — Fires notification, schedules follow-up, reschedules for next day
│   ├── FollowUpAlarmReceiver.kt   — Checks if dose taken; if not, shows "Don't forget" notification (cancels original first)
│   ├── BootReceiver.kt            — Reschedules all alarms after device reboot
│   ├── DoseActionReceiver.kt      — Handles "Taken" button: cancels both notifications + pending follow-up alarm, logs dose
│   └── NotificationHelper.kt      — Notification channel, dose reminders, follow-up reminders
├── backup/                        — BackupWorker (WorkManager periodic task)
├── util/                          — FlowUtils (combine for 8+ flows)
└── di/                            — AppModule, DatabaseModule (with migrations), RepositoryModule
```

`MedicineReminderApp.kt` is the `@HiltAndroidApp` Application class. On every `onCreate` it creates the notification channel and runs `RescheduleAllAlarmsUseCase` on a background coroutine — this catches medicines added before alarm wiring and reschedules everything after process restart.

**MVVM pattern**: Each screen has a `*Screen.kt` (Composable) and `*ViewModel.kt`. ViewModels expose `StateFlow<UiState>` and receive user actions as method calls. Use cases sit between ViewModels and repositories.

## Database Schema

Three tables. **Current version: 2**. `MIGRATION_1_2` (defined in `AppDatabase.kt`) adds `reminderWindowMinutes` and `notificationsEnabled` columns to medicines.

**`medicines`** — id (PK), name, dosage (e.g. "500 mg" — single string), frequency (`DAILY`|`SPECIFIC_DAYS`), activeDays (comma-separated day-of-week ints 1-7), notes, isActive, reminderWindowMinutes (nullable Int — `null` = use default, `-1` = disabled, otherwise minutes), notificationsEnabled (boolean), createdAt, updatedAt

**`schedules`** — id (PK), medicineId (FK→medicines, CASCADE), hour (0-23), minute (0-59)
- One row per scheduled time slot. A medicine taken 3x/day has 3 schedule rows.
- The schedule `id` is used as the PendingIntent request code for alarms — must be stable and unique.

**`dose_logs`** — id (PK), scheduleId (FK→schedules, CASCADE), date (ISO string "2026-04-15"), takenAt (epoch millis, nullable), status (`TAKEN`|`MISSED`|`SKIPPED`)
- Unique constraint on (scheduleId, date). A missing row means the dose is pending/future.

### Schema design rationale

- Separate `schedules` table (vs JSON array) enables stable foreign keys into `dose_logs` and unique PendingIntent IDs for alarms.
- `date` stored as ISO string (not epoch) avoids timezone ambiguity for date-only comparisons.
- Soft delete on medicines (`isActive = false`) preserves history. Inactive medicines still appear in History dropdown under "— Previously taken —".
- A separate hard-delete path (`MedicineRepository.permanentlyDeleteMedicine`) is used by the Clear-history flow on the History screen — clearing history also fully removes the medicine.

## Key Files

The most complex / load-bearing logic:

- `domain/usecase/doselog/GetDoseLogsForDateUseCase.kt` — Computes expected doses for a date by cross-referencing active medicines, their `activeDays` (day-of-week filter), their schedules, and existing dose logs.
- `domain/usecase/doselog/GetAdherenceStatsUseCase.kt` — Streak calculation and adherence percentage. Respects medicine creation dates (no penalty for days before a medicine existed). Defaults to **100%** when there are no scheduled doses (graceful new-user state).
- `notification/AlarmScheduler.kt` — Schedules one-shot exact alarms + follow-up alarms. Each main alarm self-reschedules after firing (via `AlarmReceiver`), enabling day-of-week filtering per fire. Uses `scheduleId.toInt()` for the main alarm PendingIntent and `scheduleId.toInt() + 500_000` for the follow-up.
- `notification/AlarmReceiver.kt` — `goAsync()` + coroutine for DB access. Checks active flags, day-of-week match, schedules follow-up.
- `notification/FollowUpAlarmReceiver.kt` — Checks if the dose has been taken; if not, fires a "Don't forget" notification. **Cancels the original dose reminder** (id = `scheduleId.toInt()`) before showing the follow-up so only one notification is visible.
- `notification/DoseActionReceiver.kt` — Cancels **both** notifications (main id `scheduleId.toInt()` and follow-up id `scheduleId.toInt() + 200_000`) plus any pending follow-up alarm via `AlarmScheduler.cancelFollowUpAlarm()`. Then logs the dose. Cancellations happen **synchronously** before `goAsync()` so the notification disappears immediately even if the DB call is slow.
- `data/repository/BackupRepositoryImpl.kt` — Closes Room DB, copies the SQLite file, uploads to Drive, reopens. Restore is the reverse + alarm rescheduling.
- `ui/screen/home/HomeViewModel.kt` — Combines flows for **3 dates** (selectedDate ± 1) into `List<DaySection>` so the home screen always shows a rolling yesterday/today/tomorrow view.
- `ui/screen/history/HistoryViewModel.kt` — Manages calendar navigation, day selection, stats, and the clear-history flow (also hard-deletes the medicine).
- `data/local/MedicineNameProvider.kt` — Lazy-loads `assets/medicines.json` once, then offers `search(query)` for autocomplete (prefers prefix matches, returns up to 20 results).

## Notification System

Alarms use `AlarmManager.setExactAndAllowWhileIdle()` (works in Doze mode). Flow:

1. User adds medicine → `AddMedicineUseCase` saves to DB and calls `AlarmScheduler.scheduleAlarm()` for each schedule.
2. `AlarmScheduler` sets a one-shot alarm with `scheduleId.toInt()` as the PendingIntent request code.
3. `AlarmReceiver.onReceive()` fires → checks `medicine.isActive`, `medicine.notificationsEnabled`, and that today's day-of-week is in `activeDays` → shows notification → schedules follow-up alarm (using `medicine.reminderWindowMinutes ?? defaultReminderWindowMinutes`) → reschedules main alarm for next day.
4. Notification has a "Taken" action button → `DoseActionReceiver` synchronously cancels both notifications + pending follow-up alarm, then logs the dose.
5. If the user doesn't tap "Taken" within the reminder window → `FollowUpAlarmReceiver` cancels the original notification, then checks if the dose was logged in the DB; if not, posts a "Don't forget: <name>" notification (id = `scheduleId.toInt() + 200_000`).
6. `BootReceiver` reschedules all alarms after device reboot.
7. `UpdateMedicineUseCase` and `DeleteMedicineUseCase` cancel old alarms (and any pending follow-ups) before scheduling new ones.

**Notification flags**: `setOngoing(false)` + `setAutoCancel(false)` + `setTimeoutAfter(0)` — notifications persist until the user either swipes them away or taps "Taken". They are NOT auto-cancelled on tap (only the action button cancels them).

**Reminder window**: Default is **2 hours** (configurable in Settings: 30m / 1h / 2h / 4h). Per-medicine override options: use default, 30m, 1h, 2h, 4h, or disabled (`-1`). Medicines can also have notifications disabled entirely.

**Notification IDs (offsets)**:
- Main dose reminder: `scheduleId.toInt()`
- Follow-up notification: `scheduleId.toInt() + 200_000` (`FOLLOW_UP_NOTIFICATION_OFFSET`)
- "Taken" action PendingIntent (main): `scheduleId.toInt() + 100_000` (`ACTION_PENDING_INTENT_OFFSET`)
- Follow-up alarm PendingIntent: `scheduleId.toInt() + 500_000` (`FOLLOW_UP_REQUEST_CODE_OFFSET`)

These offsets must NOT overlap. If you add new notification types, pick a fresh range.

**Permissions**: `SCHEDULE_EXACT_ALARM` (API 31+, prompted on launch via `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`), `POST_NOTIFICATIONS` (API 33+, prompted on first launch), `RECEIVE_BOOT_COMPLETED`.

## Screens

1. **Home** (`screen/home/`) — `CalendarStrip` (horizontal date scroller) + animated progress bar ("X of Y taken") + **3-day rolling view** (yesterday/today/tomorrow grouped sections, each with Morning/Afternoon/Evening/Night subgroups). Overdue doses tinted red. Snackbar with **Undo** on dose toggle. FAB to add a medicine. Empty days are hidden (no header for an empty day). When the user navigates via the calendar strip, the 3-day window slides with the selection.
2. **Medicines** (`screen/medicines/`) — Searchable list of all active medicines. Each card shows name, dosage, schedule times, frequency. Edit + delete buttons. **Delete uses a two-step "type yes" confirmation** (same pattern as Clear history).
3. **Add/Edit Medicine** (`screen/addeditmedicine/`) — Form fields:
   - **Name** with autocomplete dropdown (powered by `MedicineNameProvider`, kicks in at 2+ characters)
   - **Dosage**: amount field + unit dropdown (mg, g, mcg, IU, mL, drops, tablets, capsules, puffs, patches, tsp)
   - **Frequency**: radio buttons (`DAILY` / `SPECIFIC_DAYS`); day-of-week chips when `SPECIFIC_DAYS`
   - **Scheduled times**: chips with a TimePicker dialog
   - **Notifications**: enable/disable toggle + follow-up reminder window selector (Use default / 30m / 1h / 2h / 4h / Disabled)
   - **Notes** (optional)
4. **History** (`screen/history/`) — Medicine dropdown (active first, "— Previously taken —" separator, then inactive with "(inactive)" suffix). Date range chips (`Week` / `Month` / `Year` / `All Time`). Adherence % card (color-coded: green ≥80%, yellow ≥50%, red below). Current + best streak cards. Taken/Missed/Skipped/Total breakdown. Full month calendar (color-coded days, tap → bottom sheet with that day's full dose breakdown). When a specific medicine is selected, a "Clear history for <name>" button appears at the bottom (two-step confirmation: dialog → type "yes"). Clearing history **also permanently deletes the medicine** so it disappears from the list.
5. **Settings** (`screen/settings/`) — Notification toggle, default follow-up reminder window picker (30m/1h/2h/4h), Google Drive backup section (sign-in, auto-backup toggle, manual backup/restore, last backup timestamp).

## Google Drive Backup

Uses `drive.appdata` scope (hidden app-specific folder — never touches user's personal files). Backs up the raw Room SQLite file. WorkManager runs daily with network + battery constraints.

**Setup** (developer): Google Cloud Console project with Drive API enabled, plus an Android OAuth client ID (package: `com.mannat.medicine_reminder`, debug SHA-1 from `~/.android/debug.keystore`). Add your email as a test user in the OAuth consent screen — without that, sign-in fails with "Access blocked: app has not completed verification".

Get the debug SHA-1:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android | grep SHA1
```

**Restore flow**: Download DB → close Room → replace file → delete WAL/SHM → reopen Room → reschedule all alarms.

## Dosage Units

The `DosageUnit` enum in `domain/model/Medicine.kt` defines: mg, g, mcg, IU, mL, drops, tablets, capsules, puffs, patches, tsp. Dosage is stored as a single string (e.g., `"500 mg"`) with `DosageUnit.parseDosage()` (string → amount + unit) and `DosageUnit.formatDosage()` (amount + unit → string). Adding a new unit only requires adding an entry to the enum.

## Medicine Name Database

`app/src/main/assets/medicines.json` — JSON array of 13,827 medicine names, ~345KB. Built from:
- **RxNorm** generic drug names (US standard, fetched from `https://rxnav.nlm.nih.gov/REST/allconcepts.json?tty=IN`), filtered to remove pure chemical formulas
- **INN international names** that differ from US spellings (paracetamol, salbutamol, adrenaline, lignocaine, frusemide, etc.)
- **WHO Essential Medicines** spellings (aciclovir, beclometasone, cefalexin, etc.)
- **Common vitamins and supplements** (Vitamin A-K, melatonin, fish oil, biotin, ashwagandha, etc.)
- A handful of well-known brand names people instinctively search for (aspirin)

`MedicineNameProvider.search(query, limit=20)` does a case-insensitive `contains` filter, sorting prefix matches first. Loads the JSON lazily on first call and caches in memory.

## Adherence calculation

`GetAdherenceStatsUseCase`:
- Builds a per-day list (`List<DayAdherence>`), one entry per day in the range that had ≥1 scheduled dose, **for medicines that existed on that day**.
- Effective end date is `min(endDate, today)` — never counts future days.
- Total scheduled = sum across all days. Adherence = takenCount / scheduledCount × 100. **If totalScheduled == 0, adherence = 100%** (new-user-friendly).
- Streaks: walk backward through dailyBreakdown sorted descending; a day counts toward the streak only if `takenCount == scheduledCount`. Current streak = consecutive 100% days from the most-recent day; longest streak = max run.

## Adding a New Feature Checklist

1. Domain model in `domain/model/` if needed
2. Room entity update in `data/local/db/entity/` + migration in `AppDatabase.kt` if schema changes (bump version, add `Migration` object, register in `DatabaseModule.addMigrations`)
3. DAO update in `data/local/db/dao/`
4. Repository interface in `domain/repository/`, implementation in `data/repository/`
5. Use case in `domain/usecase/`
6. Hilt bindings in `di/` modules if a new repository is added
7. ViewModel + Screen in `ui/screen/`
8. Add to `NavGraph.kt` and `BottomNavItem` if it's a new tab
9. Update mappers in `data/mapper/` if entity/domain model changed
10. If your feature touches alarms, plumb through `AlarmScheduler` calls in the relevant use cases (Add/Update/Delete) and ensure the receivers respect any new settings

## Conventions

- **Destructive actions** (delete medicine, clear history) use a two-step confirmation: explanation dialog → "type yes" dialog. Pattern lives in `MedicinesScreen` and `HistoryScreen`.
- **Snackbar undo** is offered for non-destructive toggles (mark taken, mark skipped, undo).
- Date strings: ISO format (`yyyy-MM-dd`) at the DAO/storage layer. `LocalDate` everywhere in domain/UI.
- Time strings: `LocalTime` in domain. Stored as separate `hour`/`minute` ints in `schedules` so Room queries can sort by time naturally.
- All BroadcastReceivers use `goAsync()` + `CoroutineScope(Dispatchers.IO)` for DB access. Always wrap in `try { … } finally { pendingResult.finish() }`.

## Testing

```bash
./gradlew test          # Unit tests
./gradlew assembleDebug # Build debug APK
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Known Limitations / Quirks

- "Taken" from the notification logs the dose for `LocalDate.now()`. If a notification fires at 11:59 PM and the user taps Taken at 12:01 AM, it's logged for the new day. Acceptable for v1; revisit if it becomes a complaint.
- The autocomplete dropdown is shown unconditionally while suggestions exist. There's no explicit "dismiss" — the user can keep typing or pick a suggestion.
- Build can be brittle if the Gradle daemon ports clash (we have hit `Could not connect to the Gradle daemon` when running inside heavily sandboxed environments — running directly in Terminal works fine).
