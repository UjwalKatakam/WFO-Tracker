# 🏢 WFO Tracker (Work From Office Companion)

A comprehensive, offline-first Android application built with modern **Kotlin** and **Jetpack Compose (Material Design 3)** to manage, track, and optimize hybrid work and Return-to-Office (RTO) compliance.

WFO Tracker eliminates the friction and uncertainty of workplace attendance tracking through live foreground chronometers, smart geofencing, arrival transit offsets, departure reminders, native calendar and time pickers, vacation/leave management, glanceable home screen widgets, and cloud backup via GitHub Gist.

---

## 🌟 Key Highlights & Capabilities

- **⏱️ Live Chronometer & Target Tracking**: Real-time foreground service chronometer that counts elapsed office time or remaining countdown against daily quota (e.g., 6.0h / 8.0h) with persistent notifications.
- **📱 Glanceable Home Screen Widget**: AppWidget with live status indicators and one-tap Punch In / Punch Out shortcuts directly from your launcher.
- **📅 Interactive Calendar & 12-Month Matrix**:
  - **Month Focus View**: Smooth month-by-month navigation with working `<` and `>` arrows, month quick-dropdown picker, and "Jump to Today" shortcut.
  - **12-Month Matrix View**: Macro-level annual overview showing monthly compliance counts, leave badges, and one-tap month focusing without dead controls.
  - **Long-Press Quick Actions**: Long-press any date cell to immediately open the **Manage Day** popup to log attendance, record leaves, or edit existing records.
- **🌴 Integrated Leaves & Holiday Exclusions**:
  - Log Public Holidays, Sick Leaves, Vacations, Floater Holidays, or Approved WFH.
  - Automatically deducts excused days from the monthly quota calculation, ensuring streak counters and compliance percentages remain 100% accurate.
- **✍️ Manual Attendance & Leave Logging**:
  - Dual-tab unified dialog: **[ 🏢 Office Work ]** and **[ 🌴 Leave / Holiday ]**.
  - Powered by native Android `DatePickerDialog` and `TimePickerDialog` (no error-prone text boxes).
  - Real-time duration calculation, validation, and optional notes.
- **📍 Smart Geofence & Timing Assistance**:
  - Configurable office GPS location and detection radius with one-tap location auto-detect.
  - Optional arrival notification with customizable transit offset (e.g., +15m or +30m transit allowance).
  - Departure reminder ("Did you forget to punch out?") with smart backdated punch-out options (-15m, -30m, -1h).
  - Instant Undo Punch Out banner in case of accidental clicks.
  - Weekend (Sat/Sun) and approved holiday auto-muting.
- **📊 Analytics & Visual Insights**:
  - WFO frequency run rate calculation (required days per remaining weeks).
  - Weekly consistency streak counter.
  - Arrival time consistency histogram (punctuality distribution).
  - Weekly hours and attendance trends.
- **☁️ GitHub Gist Cloud Sync & Data Freedom**:
  - Full two-way sync to a private GitHub Gist using Personal Access Tokens (PAT).
  - Export data as CSV, PDF, or JSON backup files.
  - JSON import with merge conflict prevention.

---

## 📱 Screen-by-Screen Tour

### 1. 🏠 Dashboard Screen
The primary command center for daily office attendance:
- **Active Timer Card / Progress Ring**: Displays live elapsed time or countdown to daily quota with an animated circular progress indicator.
- **Quick Punch Actions**:
  - Large **Punch In** / **Punch Out** buttons.
  - Arrival prompt with transit offset selector (+0m, +15m, +30m, or custom arrival time picker).
  - Departure prompt with backdated punch-out choices (-15m, -30m, -60m, or custom departure time).
  - Undo banner with countdown timer to restore an accidentally closed session.
- **Monthly Compliance Card**:
  - Valid office days completed vs. required days (auto-adjusted for leaves).
  - Quota run rate indicator (`X days / week needed` or `Quota Met! 🎉`).
  - Weekly streak counter and current week progress.
  - Quick badge to view and manage approved leaves/holidays.
- **Punctuality & Trends**:
  - Arrival histogram displaying distribution of check-in times against shift start.
  - Trend charts illustrating weekly total hours and compliant days.
- **Interactive Monthly Calendar**:
  - Visual color-coded cells: 🟢 Valid WFO Day, 🟡 Under-Target Shift, 🟣 Holiday/Leave, ⚪ Weekend.
  - Tap any date to view its daily timeline below; long-press any date to quickly add or edit logs and leaves.

### 2. 📜 Log History Screen
Complete audit trail of all recorded office sessions:
- **Top Metric Cards**: High-contrast summary cards showing Valid WFO count, Under-Quota count, Total Hours logged, and Average Shift Duration.
- **Search & Filters**: Real-time search bar by notes or date string, paired with status filter chips (**All**, **Valid WFO**, **Under Target**).
- **Date Range Picker**: Filter logs by start and end dates with clearable chips.
- **Data Actions**:
  - **+ Manual Log**: Opens the unified attendance and leave dialog.
  - **Export Options**: Export records to CSV, PDF summary, or raw JSON.
  - **Import JSON**: Restore logs from backup files.
- **Log List Cards**: Each entry details date, formatted 12-hour arrival/departure times, total duration, compliance status pill, notes, and direct Edit / Delete actions.

### 3. 📅 Calendar & WFO Matrix Screen
Dedicated macro-level view of your hybrid work schedule:
- **View Mode Switcher**:
  - **Month Focus**: In-depth monthly calendar with working `<` (Previous Month) and `>` (Next Month) navigation that properly rolls over years across December/January. Includes month dropdown picker, horizontal month chips (`Jan`–`Dec`), and a "Jump to Today" button.
  - **12-Month Matrix**: Compact, scrollable grid of all 12 months for the selected year with WFO count, leave counts, and direct `Focus ↗` buttons.
- **Daily Status Timeline**:
  - Shows chronological milestones for the selected date (Office Arrival, Target Completion, Office Departure).
  - Features an inline **+ Log / Edit Day** action button.
- **Long-Press Interaction**: Long-press any date to open the **Manage Day** dialog to add punches or register leaves.

### 4. ⚙️ Settings Screen
Personalize your attendance rules, geofence, and cloud integrations:
- **Profile & Quota Configuration**:
  - Employee display name.
  - Target minimum hours per day (e.g., 6.0h).
  - Target days per week (e.g., 3 days).
  - Official shift start time (e.g., 09:30 AM).
  - Milestone notification switch on reaching daily target.
- **Location & Geofencing Intelligence**:
  - Office GPS Coordinates (Latitude, Longitude) and Geofence Radius (km).
  - **Use Current Location** button to automatically lock in current GPS position.
  - **GPS Verification**: Require being within office radius before punch-in (with manual override prompt).
  - **Passive Geofence Arrival Prompt**: Prompts you to punch in when arriving near the office.
  - **Departure Reminder**: Prompts you to punch out if you leave the office without ending your session.
  - **Auto-Mute on Weekends & Leaves**: Silences reminders on non-working days.
- **Cloud Backup (GitHub Gist)**:
  - Personal Access Token (PAT) and Gist ID configuration.
  - **Push to Gist** (encrypts/uploads attendance logs and exclusions).
  - **Pull from Gist** (merges cloud records into local database).
- **Data Management**:
  - CSV / PDF export triggers.
  - Full data wipe with confirmation dialog.

---

## 🌴 How Leaves & Holidays Impact WFO Quota

WFO policies typically mandate office presence based on available working days in a month. WFO Tracker handles this dynamically:

$$\text{Required Days} = \max(0, \text{Target Days Per Month} - \text{Approved Working Day Leaves})$$

- **Weekend Exclusions**: Weekends (Saturday and Sunday) are never counted against your quota.
- **Weekday Leaves**: When a Public Holiday or approved leave (Sick, Vacation, Floater, WFH) falls on a weekday (Monday–Friday), it automatically reduces your required office days.
- **Visual Distinction**:
  - 🌴 **Holiday**: Public or company calendar holiday.
  - 🤒 **Sick**: Medical or wellness leave.
  - ✈️ **Vacation / Paid Leave**: Annual or casual time off.
  - 🏠 **Approved WFH**: Excused work-from-home exemption.

---

## 🏗️ Technical Architecture & Tech Stack

```
com.example/
├── data/
│   ├── local/               # Room Database (Entities: CheckInLog, LeaveHoliday; DAOs)
│   ├── preferences/         # User preferences and WFO settings
│   └── repository/          # Unified WfoRepository (Room, Gist API, exports)
├── service/
│   ├── PunchTimerService    # Foreground Service with ongoing notification & live timer
│   └── PunchActionReceiver  # BroadcastReceiver for notification and widget click actions
├── ui/
│   ├── components/          # Reusable Jetpack Compose UI components
│   │   ├── CalendarComponents.kt   # MonthCalendarCard, YearMatrixMonthCard, DailyStatusTimelineCard
│   │   ├── ManualLogDialog.kt      # Unified Attendance & Leave Dialog with native pickers
│   │   ├── DateActionDialog.kt     # Long-press action popup for calendar dates
│   │   ├── LeavesDialog.kt         # Dedicated leave management modal
│   │   └── CircularProgressTimer.kt # Animated progress ring chronometer
│   ├── screens/             # Top-level composable screens
│   │   ├── DashboardScreen.kt      # Live chronometer, metrics, trends, quick calendar
│   │   ├── LogHistoryScreen.kt     # Full log audit table, filters, search, exports
│   │   ├── CalendarArchiveScreen.kt# Month focus & 12-month year matrix
│   │   ├── SettingsScreen.kt       # Geofencing, quota rules, GitHub Gist sync
│   │   └── MainAppScaffold.kt      # Navigation bar, insets, runtime permission handling
│   ├── theme/               # Material 3 Color Schemes, Typography, Shapes
│   └── viewmodel/           # WfoViewModel & immutable WfoUiState
├── util/                    # DateUtils, ExportUtils (CSV/PDF), GeofenceHelper
└── widget/                  # WfoAppWidgetProvider (Android Home Screen Widget)
```

### Core Technologies
- **Language**: Kotlin 2.0+ with Kotlin Coroutines & `StateFlow`.
- **UI Toolkit**: Jetpack Compose with Material Design 3 (M3).
- **Database**: Android Room (SQLite) with KSP symbol processing.
- **Dependency Injection**: Clean Constructor Injection with Android `ViewModelProvider`.
- **Background Tasks**:
  - `PunchTimerService`: Android 14+ compatible `specialUse` Foreground Service.
  - Android `AppWidgetProvider` for launcher integration.
- **Networking**: `OkHttp` & `HttpURLConnection` for GitHub Gist REST sync.
- **Location**: Google Play Services Location (`FusedLocationProviderClient`).

---

## 🔒 Permissions & Privacy

WFO Tracker is designed with an **offline-first, privacy-first** philosophy. No personal data is sent to external servers unless you explicitly configure GitHub Gist sync with your personal token.

| Permission | Purpose |
|---|---|
| `POST_NOTIFICATIONS` | Displays the persistent foreground chronometer and target alerts (Android 13+). |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Verifies whether you are within office geofence boundaries (optional; can be disabled). |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keeps the live attendance timer running reliably without being terminated by OS battery optimization. |
| `VIBRATE` | Provides haptic feedback upon punching in, punching out, or reaching daily quota. |
| `INTERNET` & `ACCESS_NETWORK_STATE` | Only used when explicitly syncing with your private GitHub Gist. |

---

## 🚀 Getting Started & Build Instructions

### Prerequisites
- **Android Studio Ladybug (2024.2+)** or newer.
- **JDK 17** or **JDK 21**.
- Android SDK **35** (compileSdk & targetSdk), minimum supported version **Android 8.0 (API 26)**.

### Building the App
1. Clone or open the project root in Android Studio.
2. Ensure Gradle synchronization completes cleanly.
3. To build and install via Gradle:
   ```bash
   gradle assembleDebug
   ```
4. To run local unit tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## 💡 Tips & Shortcuts

1. **Long-Press Calendar Dates**: Long-press any date on either the Dashboard or Calendar screen to bring up the **Manage Day** sheet with prefilled options to log attendance or record a leave.
2. **Transit Offset**: If you punched in after arriving at your desk, select `+15m` or `+30m` in the arrival prompt to credit your transit/elevator time.
3. **Accidental Punch Out**: Tap **[Undo Punch Out]** on the top banner within 60 seconds of punching out to immediately restore your live timer session.
4. **Desktop Launcher Widget**: Long-press on your Android home screen, select **Widgets**, and add **WFO Tracker** for one-tap punch in/out and live progress tracking without opening the app.
