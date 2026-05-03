# MedCheck

Android medicine reminder app. Add medicines with dosage, frequency, and scheduled times; check off doses each day; get exact-time notifications with follow-up reminders if you forget; track adherence with streaks and a calendar heatmap; back up to Google Drive.

For developer documentation see [CLAUDE.md](CLAUDE.md).

## Quick Start

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_HOME=~/Library/Android/sdk
./gradlew assembleDebug
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and Android SDK 35.

## Features

- Add/edit/delete medicines with name (autocomplete from 13,800+ drug names), dosage (amount + unit picker), frequency (daily or specific days), and scheduled times
- Daily checklist showing **yesterday + today + tomorrow** so dose rollover at midnight is graceful
- Exact-time notifications via AlarmManager — survive reboots, persist until dismissed
- Configurable follow-up reminders (default 2h, per-medicine override, or off)
- Per-medicine notification toggle
- History with Week/Month/Year/All-time stats, current and longest streaks, color-coded month calendar with day-detail bottom sheet
- Per-medicine history filter (including inactive medicines)
- Clear-history flow with two-step confirmation that also permanently removes the medicine
- Daily Google Drive backup (`drive.appdata` scope) and one-tap restore
- Material Design 3 with dynamic color (Android 12+)
