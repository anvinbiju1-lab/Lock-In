# 🔒 Lock In - Smart App Blocker, Habit & Sleep Tracker

**Lock In** is a modern, privacy-focused Android digital wellbeing application designed to help you regain focus, build daily habits, manage screen time, and maintain healthy sleep routines. Built entirely with **Kotlin** and **Jetpack Compose (Material 3)**, it combines strict app blocking, flexible habit tracking, sleep schedule monitoring, and comprehensive focus statistics in a cohesive, dark-themed experience.

---

## ✨ Key Features

### 1. 🛡️ Strict App Blocker & Screen Time Management
- **Daily Usage Limits**: Set maximum daily usage thresholds (in minutes) for distracting apps.
- **Scheduled Lockout Windows**: Configure custom start and end time windows (e.g., during work hours or study sessions) to block specific apps.
- **Full-Screen Lockout Overlay**: Displays remaining daily limits, current usage time, and prevents app usage when limits or schedules are exceeded.
- **5-Minute Emergency Bypass**: Provides an intentional 5-minute unlock window with a countdown timer for emergency access without disabling overall rules.
- **Battery-Aware Foreground Monitor**: Intelligent foreground monitoring service that automatically sleeps when the device screen is off to preserve battery life.

### 2. 📅 Habit Tracker & Routine Builder
- **Custom Habits**: Create habits with customizable color accents, descriptions, and target reminder times.
- **Interactive Calendar & Streaks**: Track daily completion streaks, view historical logs via a monthly calendar grid, and inspect day-by-day logs.
- **Interactive Notifications**: Background reminders scheduled via Android WorkManager with quick actions directly from the notification tray (Complete / Skip).
- **Fast Management**: Single-tap completion toggles and effortless habit deletion.

### 3. 🌙 Sleep Schedule & Bedtime Focus
- **One-Tap Sleep & Wake Logging**: Log sleep and wake times with a single tap, supporting both normal and post-midnight (e.g., 12:30 AM – 5:00 AM) bedtime calculations.
- **Sleep Quality & Score Tracking**: Automatically calculates sleep duration, target deviation, and provides a daily sleep quality score.
- **Manual Time Adjustments**: Time-picker dialogs allow quick adjustments if you forget to tap sleep/wake at the exact moment.
- **Bedtime Focus Integration**: Option to automatically enforce focus shield protections while you are asleep.

### 4. 📊 Focus Stats & Digital Wellbeing Insights
- **Screen Time Analytics**: Track total daily screen time compared against restricted app usage.
- **App Usage Breakdown**: Visual progress bars showing usage against configured daily limits.
- **Habit Performance**: Historical completion counters and active streak insights.
- **Emergency Unlock Log**: Audit log of past emergency unlocks to foster digital mindfulness.

---

## 🛠️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material Design 3 (M3)](https://m3.material.io/)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern + Clean Architecture
- **State Management**: Kotlin Coroutines & `StateFlow` / `collectAsStateWithLifecycle`
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) (SQLite) with KSP compiler
- **Background Tasks & Scheduling**:
  - Android `ForegroundService` with `specialUse` type for usage monitoring
  - Android `WorkManager` for daily habit reminder scheduling
  - `BroadcastReceiver` for boot initialization and notification actions
- **Usage & System APIs**: Android `UsageStatsManager`, `PowerManager`, `NotificationManager`, `WindowManager`
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with Gradle Version Catalog (`libs.versions.toml`)
- **Testing**: Robolectric, Roborazzi UI/Screenshot Testing, JUnit 4, Compose UI Test

---

## 📁 Project Structure

```text
app/src/main/java/com/example/
├── FocusLockApp.kt             # Application class: Room DB & Notification channels setup
├── MainActivity.kt             # Main entry point with Compose edge-to-edge navigation
├── data/
│   ├── AppDatabase.kt          # Room Database definition
│   ├── dao/                    # Data Access Objects (Habits, Sleep, App Blocker)
│   ├── entity/                 # Room Entities (HabitEntity, SleepLogEntity, AppRestrictionEntity, etc.)
│   └── repository/             # Repositories for data operations and business rules
├── receiver/
│   ├── BootReceiver.kt         # Restores services and alarms on device reboot
│   └── HabitActionReceiver.kt  # Handles quick notification actions (Done/Skip)
├── service/
│   └── AppMonitorService.kt    # Foreground service monitoring active apps & screen state
├── ui/
│   ├── MainScreen.kt           # Bottom navigation bar & tab orchestrator
│   ├── blocker/                # App blocker screens, rule config dialog, and Lockout Activity
│   ├── habits/                 # Habit list, add habit dialog, calendar, and day details
│   ├── sleep/                  # Sleep tracking dashboard, live clock, and sleep statistics
│   ├── stats/                  # Focus stats, screen time charts, and emergency unlock logs
│   ├── onboarding/             # Permission grant & onboarding flow
│   └── theme/                  # M3 Dark color palette, typography, and shape styling
├── util/
│   ├── DateUtils.kt            # Date/time helpers and formatting
│   ├── HabitNotificationHelper.kt # Notification builders with action intents
│   └── UsageStatsHelper.kt     # UsageStatsManager & system permission verification helpers
└── worker/
    └── HabitReminderWorker.kt  # WorkManager periodic worker for habit reminders
```

---

## 🔐 Required Android Permissions

| Permission | Purpose |
|---|---|
| `PACKAGE_USAGE_STATS` | Reads daily app screen time and identifies the currently active foreground application to enforce time limits. |
| `SYSTEM_ALERT_WINDOW` | Displays the lockout screen over restricted apps when usage limits or lockout schedules are triggered. |
| `POST_NOTIFICATIONS` | Delivers habit reminders and maintains the ongoing foreground service status notification. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Ensures the app monitor service continues running reliably in the background without being killed by OS battery managers. |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` | Runs the continuous background monitor with Android 14+ foreground service standards. |
| `RECEIVE_BOOT_COMPLETED` | Automatically restarts habit alarms and monitoring services when the phone boots up. |
| `QUERY_ALL_PACKAGES` | Allows selecting and configuring limits for installed user applications. |

---

## 🚀 Getting Started & Build Instructions

### Prerequisites
- **Android Studio** Ladybug (2024.2.1) or newer
- **JDK 17** or **JDK 21**
- **Android SDK** API 36 (Minimum SDK: 24 / Android 7.0)

### Clone & Build
1. Clone the repository:
   ```bash
   git clone https://github.com/anvinbiju1-lab/Lock-In.git
   cd Lock-In
   ```

2. Open the project in **Android Studio**.

3. Let Gradle sync dependencies automatically.

4. Build the debug APK via Gradle or Android Studio:
   ```bash
   ./gradlew assembleDebug
   ```

5. Run the app on a connected physical device or emulator running Android 7.0+ (API 24+).

> **Note on Permissions**: When launching the app for the first time, navigate to the **Blocker** tab and grant **Usage Access**, **Draw Over Other Apps**, and **Notification** permissions so the monitor service can function properly.

---

## 🧪 Testing

Run JVM unit and Robolectric tests:
```bash
./gradlew testDebugUnitTest
```

Run Roborazzi screenshot verification tests:
```bash
./gradlew verifyRoborazziDebug
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) .
