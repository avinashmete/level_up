# LevelUp

> Turn your real life into an RPG. Main quests, side quests, daily missions, XP, ranks, and an optional AI Quest Forge — built once in Kotlin + Compose, running on **Android** and **iOS**.

## Architecture

LevelUp is a **Compose Multiplatform** app. ~95% of the code (UI, data, business logic) lives in `:shared/commonMain`. Each platform contributes a tiny host:

```
┌──────────────────┐     ┌──────────────────┐
│  androidApp/     │     │  iosApp/         │
│  MainActivity    │     │  iOSApp.swift    │
│  + BackupBridge  │     │  + BackupBridge  │
└────────┬─────────┘     └────────┬─────────┘
         │                        │
         └──── :shared ───────────┘
                 │
   ┌─────────────┴───────────────┐
   │ commonMain (Compose UI,     │
   │ Room-style data via         │
   │ SQLDelight, Ktor, prefs,    │
   │ domain logic, AI forge)     │
   └─────────────────────────────┘
```

## Features

- **Main quests** — long-arc life goals.
- **Side quests** — one-off bounties.
- **Daily missions** — recurring habits with streak tracking.
- **XP curve + Levels (L1–L99)** with non-linear progression.
- **Hunter Ranks** — `E → D → C → B → A → S`.
- **Stats** — `STR`, `INT`, `DIS`, `VIT`, `SOC` grow with themed quests.
- **Achievements** — first blood, streak milestones, rank promotions.
- **Quest Forge** — turn a goal into a set of quests.
  - Offline template generator (default, no network).
  - Optional **OpenAI BYO-key** mode (stored on-device).
- **Local-first storage** with cross-platform JSON export / import.

## Tech stack

| Layer       | Library                                           |
| ----------- | ------------------------------------------------- |
| UI          | Compose Multiplatform 1.7 + Material 3            |
| Persistence | SQLDelight 2.x (SQLite on both platforms)         |
| Preferences | `multiplatform-settings`                          |
| HTTP        | Ktor (OkHttp engine on Android, Darwin on iOS)    |
| Date/time   | `kotlinx-datetime`                                |
| Serialization | `kotlinx-serialization-json`                    |
| Language    | Kotlin `2.0.21`, AGP `8.7.0`, minSdk 26 / iOS 16+ |

## Project layout

```
LevelUp/
├── build.gradle.kts            # root: declares plugin versions
├── settings.gradle.kts         # includes :shared, :androidApp
├── gradle.properties
├── shared/                     # 95% of the code lives here
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/
│       │   ├── sqldelight/.../LevelUpDatabase.sq   # schema + queries
│       │   └── kotlin/com/levelup/app/
│       │       ├── AppContainer.kt                  # manual DI
│       │       ├── ai/QuestSuggester.kt
│       │       ├── data/{Models,LevelUpRepository}.kt
│       │       ├── data/backup/Backup.kt
│       │       ├── data/db/DriverFactory.kt         # expect
│       │       ├── data/prefs/UserPreferences.kt
│       │       ├── domain/{Progression,DailyReset,Rewards}.kt
│       │       ├── platform/{BackupBridge,Toaster}.kt
│       │       └── ui/                              # full Compose UI
│       ├── androidMain/                             # SQLDelight Android driver
│       ├── iosMain/                                 # SQLDelight Native driver + MainViewController
│       └── commonTest/                              # pure-Kotlin unit tests
├── androidApp/                 # thin Android host
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/levelup/app/android/
│           ├── LevelUpApplication.kt
│           ├── MainActivity.kt
│           └── AndroidBackupBridge.kt
└── iosApp/                     # thin iOS host (Xcode project added per-machine)
    ├── README.md               # how to attach the Xcode project
    ├── iosApp/
    │   ├── iOSApp.swift
    │   ├── IOSBackupBridge.swift
    │   └── Info.plist
    └── Configuration/Config.xcconfig
```

## Build & run

### Prerequisites

- **Android Studio Iguana+** (or IntelliJ IDEA with the KMP plugin).
- **JDK 17** (set in Android Studio's Gradle JDK settings).
- **Xcode 16+** (iOS only, macOS only).
- **Kotlin Multiplatform Mobile** plugin in Android Studio (recommended).

### Android

```bash
./gradlew :androidApp:installDebug
```

Or open the project in Android Studio and run the `androidApp` configuration on an emulator or device (API 26+).

### iOS

The iOS Xcode project is intentionally **not committed** — its `project.pbxproj` contains absolute, per-machine paths. See `iosApp/README.md` for the one-time setup:

1. Build the shared framework with `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`.
2. Create a new SwiftUI app in Xcode under `iosApp/`, drop in `iOSApp.swift` and `IOSBackupBridge.swift`.
3. Add a Run Script phase that calls `./gradlew :shared:embedAndSignAppleFrameworkForXcode`.
4. Link `shared.framework` from `shared/build/xcode-frameworks/...`.
5. Run on an iPhone simulator (iOS 16+).

The fast path: install the **Kotlin Multiplatform** plugin in Android Studio and let it generate the iOS Xcode project for you.

## AI Quest Forge

Default is offline: a keyword-driven template generator. To use a real model:

1. Open **System → AI Quest Generator** and pick **OpenAI**.
2. Paste an `sk-...` key. It is stored in `multiplatform-settings` on the device.
3. Optionally change the model (default `gpt-4o-mini`).

If the API call fails (no network, bad key, rate limit), the Forge falls back to offline suggestions automatically. The app **never** sends data anywhere unless AI mode is on and you tap *Forge*.

## Backup

`System → Backup → Export` writes a `levelup-backup.json` file using each platform's native file picker (Storage Access Framework on Android, `UIDocumentPicker` on iOS). *Import* reads any compatible `levelup-backup.json` back into the app.

## Roadmap

- Notifications + daily quest reminders (per-platform)
- Charts for XP-per-day and stat trends
- Cloud sync (optional)
- Home-screen widgets (Android Glance, iOS WidgetKit)

## Disclaimer

LevelUp is inspired by the RPG-progression *idea* found in stories like *Solo Leveling*, but does not use any copyrighted names, characters, art, or trademarks from those properties.
