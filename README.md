# Beast App

A workout tracker designed specifically for tracking progress when working with pre-made workout programs like "Beach Body".

## Overview
Beast App is an Android-first KMM project focused on: 
- Fast logging of strength workouts for 90-day and other structured programs
- Progress tracking (photos, measurements, simple charts)
- Calendar + reminders (local notifications)
- Local-first privacy with future sync options

## Tech stack
- Android UI: Jetpack Compose + Material 3, Navigation Compose, Hilt DI, WorkManager
- Shared (KMM): Kotlin Multiplatform (commonMain), Kotlinx Coroutines/Serialization
- Data (Android): Room database
- Networking: Ktor (planned)

## Project status (MVP ramp-up)
- Init & Gradle: Done (Kotlin 2.0.21, AGP 8.6, Compose Compiler plugin configured)
- Structure & models: Done (Program, WorkoutDay, Exercise, SetLog, WorkoutLog)
- Repositories (Android/Room): Done
- Use cases: Core implemented (load programs, plan 90-day cycle, log workouts, calculate progress); reminders API prepared
- UI Nav: Done (BottomNavigation, 5 sections stubbed)
- Programs screen: In progress (VM + empty state, seeding to be added)
- Reminders (WorkManager): Done (scheduler + worker)

## Project layout
```
BeastApp/
 ├─ androidApp/        # Android app (Compose/Hilt/Navigation)
 ├─ shared/            # KMM shared module (models, use cases, repositories)
 ├─ build.gradle.kts   # Root Gradle
 ├─ settings.gradle.kts
 ├─ gradle/            # Versions catalog, wrapper files
 ├─ Main_Protocol.md   # Development log
 ├─ TODO.md            # Task plan
 └─ specs.md           # Functional spec
```

## Getting started
- Open the project in Android Studio (with Kotlin 2.0.x support)
- Sync Gradle; ensure Online mode
- Run the app configuration for `androidApp`

Optional CLI build (Windows):
```
cd C:\VibeCoding\BeastApp
gradle wrapper
.\u200bgradlew.bat :androidApp:assembleDebug
```

Notes:
- On Android 13+ the app requests POST_NOTIFICATIONS permission for reminders.

## Roadmap (short)
- Onboarding (units, base params, pick/start program)
- Seed/import a 90-day program
- Dashboard with progress and next workout CTA
- Backup/export/import utilities

## License
TBD

