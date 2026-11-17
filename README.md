# WFO Tracker

WFO Tracker is a personal Android app for tracking in-office attendance days against a monthly quota. It records active sessions, counts qualified days after leave and holiday exclusions, and stores records locally with optional GitHub Gist backup.

## Features

- **Tracking**: One-tap punch in at the current time and punch out, "Arrived earlier" backdating, a live foreground timer with target departure time, an overdue departure reminder, and undo punch out.
- **Quota and calendar**: Monthly quota of valid days based on minimum hours per day, month view and 12-month matrix, weekly streak tracking, and leave or holiday types (`holiday`, `sick`, `vacation`, and `WFH`).
- **Holidays**: Manual weekday range entry, `.ics` calendar file or pasted-date import with candidate preview, and undo for deletes and batch imports.
- **Reports and data**: History search and status/date filters, CSV share, single-month PDF report export, JSON file import/export, and GitHub Gist backup and restore.
- **Automation**: Periodic office geofence arrival prompt, a home-screen widget showing status and elapsed time, and target/departure notifications.

## Design

The interface uses a "quiet utility" palette built around dark and light surfaces separated by tone and 1 dp borders (`rule`), with `present` as the sole accent. Dark mode tokens use `#15171A` (`ground`), `#1E2126` (`raised`), `#2D3138` (`rule`), `#ECEDEF` (`ink`), `#8B919A` (`muted`), `#5BB5A2` (`present`), `#E0A458` (`short`), and `#E5655A` (`danger`). Light mode tokens use `#F5F6F8` (`ground`), `#FFFFFF` (`raised`), `#DFE2E6` (`rule`), `#15171A` (`ink`), `#5E6570` (`muted`), `#247564` (`present`), `#9E5D0A` (`short`), and `#C53328` (`danger`).

Typography uses static local IBM Plex Sans fonts (Regular, Medium, SemiBold) with tabular numerals (`tnum`) across a 12–56 sp scale. Pill buttons and filter chips use `CircleShape`, grouped cards and bottom sheets use 12 dp rounded corners, and calendar cells use 4 dp corners.

Layout and copy follow strict rules: one accent color, no gradients or drop shadows, sentence case labels, 48 dp minimum touch targets, and at least 4.5:1 text contrast. Theme definitions live in [Color.kt](app/src/main/java/com/example/ui/theme/Color.kt), [Type.kt](app/src/main/java/com/example/ui/theme/Type.kt), [Shape.kt](app/src/main/java/com/example/ui/theme/Shape.kt), and [Theme.kt](app/src/main/java/com/example/ui/theme/Theme.kt).

## Architecture

- **Language and UI**: Kotlin with Jetpack Compose and Material 3.
- **Persistence**: Room (`wfo_database.db`) for attendance logs and leaves; Preferences DataStore (`wfo_settings.preferences_pb`) for settings and active punch state.
- **Background work**: WorkManager (`GeofencePeriodicWorker` every 15 minutes, `GistSyncWorker` every 6 hours), `PunchTimerService` foreground service, and `WfoAppWidgetProvider` AppWidget.

```text
app/src/main/java/com/example/
├── data/           # Room database/DAOs, Retrofit Gist API, DataStore prefs, WfoRepository
├── service/        # Foreground timer, notification/widget receivers, periodic geofence worker
├── ui/
│   ├── components/ # Shared controls, dashboard cards, calendar grid, dialogs, pickers
│   ├── screens/    # Dashboard, History, Calendar archive, Settings, scaffold
│   ├── theme/      # Colors, IBM Plex Sans typography, shapes, Material 3 theme
│   └── viewmodel/  # WfoViewModel and WfoUiState
├── util/           # Date helpers, Keystore AES-GCM encryption, ICS parser, PDF report, GPS
├── widget/         # WfoAppWidgetProvider for home screen punch status
└── work/           # Periodic GitHub Gist sync worker
```

## Data and privacy

Attendance logs (`CheckInLog`) and leave/holiday exclusions (`LeaveHoliday`) live locally in Room, while policy targets, office coordinates, Gist ID, active punch start time, and undo state live in Preferences DataStore.

Your GitHub personal access token is encrypted with a 256-bit AES-GCM key stored in the Android Keystore and is never persisted in plain text. If decryption fails on read, the app clears the readable state and asks you to re-enter the token in Settings.

Office coordinates and radius are used on-device only for punch-in geofence checks and periodic arrival alerts. Android backup rules include `wfo_database.db` and `wfo_settings.preferences_pb`, so device backups restore history and settings, though a Keystore-encrypted token cannot be read on a new device and must be entered again. When Gist sync is configured, attendance logs, leaves, and non-secret policy settings are uploaded to that Gist, so keep the Gist private.

## Build and run

Open the project in Android Studio (JDK 11+ / SDK 36) with `minSdk = 24`, `targetSdk = 36`, and `compileSdk = 36`.

```bash
gradle assembleDebug          # Build debug APK
gradle :app:testDebugUnitTest # Run unit and Robolectric tests
```

| Permission | Purpose |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Sync logs, leaves, and policy settings with GitHub Gist when configured. |
| `POST_NOTIFICATIONS` | Show the live punch timer, target alerts, departure reminders, and arrival prompts. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the punch-in timer running in the background (`PunchTimerService`). |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Check proximity to the configured office coordinates on punch in and periodic arrival checks. |
| `VIBRATE` | Deliver tactile feedback and notification alert vibrations. |
| `RECEIVE_BOOT_COMPLETED` | Restore the active timer and periodic geofence work after device reboot or app update. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prompt the user to exempt the app from battery limits so timer alerts are not delayed. |

## Limits

- Periodic arrival checks are scheduled via WorkManager (15-minute minimum interval), so arrival alerts are not instant.
- Saturdays and Sundays are always treated as non-working days and are never inserted as leave days.
- The app models a single user profile and one set of office coordinates.
- Monthly PDF exports fit one page and cap the table at 18 log rows, noting any remaining entries below the table.
