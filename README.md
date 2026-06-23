# LevelUp

> Turn your real life into an RPG. Main quests, side quests, daily missions, XP, ranks, and an optional AI Quest Forge.

LevelUp is an Android app, written in Kotlin + Jetpack Compose, that gamifies your goals
and habits. It is local-first: your data stays on your device unless you export it to a
JSON backup file you control.

## Features

- **Main quests** — long-arc life goals (e.g. *Get fit*, *Learn Japanese*).
- **Side quests** — one-off bounties (e.g. *Run a 5K*, *Read a book*).
- **Daily missions** — recurring habits that reset every day and build a streak.
- **XP + Levels (L1–L99)** with a non-linear curve so each level feels earned.
- **Hunter Ranks** — `E → D → C → B → A → S`, unlocked at level milestones.
- **Stats** — `STR`, `INT`, `DIS`, `VIT`, `SOC` grow as you complete themed quests.
- **Achievements** — first blood, streak milestones, rank promotions, etc.
- **Quest Forge** — type a life goal and the app suggests quests for you.
  - **Offline mode (default)**: deterministic template generator. No network.
  - **OpenAI mode (opt-in)**: paste your own API key, model is configurable.
- **Local-first storage** with `Room` and JSON `export` / `import` backup.

## Tech stack

| Layer       | Choice |
| ----------- | ------ |
| UI          | Jetpack Compose + Material 3 |
| Navigation  | `androidx.navigation:navigation-compose` |
| Persistence | Room (SQLite) |
| Preferences | DataStore Preferences |
| Serialization | `kotlinx.serialization-json` |
| AI client | OkHttp + OpenAI Chat Completions (optional, BYO key) |
| Language    | Kotlin `2.0.20`, Android Gradle Plugin `8.7.0`, minSdk 26, targetSdk 34 |

## Build and run

Easiest path is **Android Studio (Iguana or newer)**.

1. Install **Android Studio** with the Android SDK 34 platform.
2. Open this folder. Studio will offer to download Gradle and create the Gradle wrapper.
3. Connect an Android device (or start an emulator with API level ≥ 26).
4. Run the `app` configuration.

If you prefer the command line, install a Gradle 8.7+ distribution and run:

```bash
gradle wrapper                  # one-time: generate gradlew + wrapper jar
./gradlew :app:installDebug     # build and install on a connected device
```

## Project layout

```
app/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── java/com/levelup/app/
    │   │   ├── LevelUpApplication.kt   # process-wide DI bootstrap
    │   │   ├── MainActivity.kt
    │   │   ├── ai/
    │   │   │   └── QuestSuggester.kt   # OpenAI + offline template generator
    │   │   ├── data/
    │   │   │   ├── *.Entity.kt         # Room entities
    │   │   │   ├── Daos.kt
    │   │   │   ├── LevelUpDatabase.kt
    │   │   │   ├── LevelUpRepository.kt # completion transaction
    │   │   │   ├── LevelUpContainer.kt  # manual DI
    │   │   │   ├── backup/Backup.kt     # JSON export/import
    │   │   │   └── prefs/UserPreferences.kt
    │   │   ├── domain/
    │   │   │   ├── Progression.kt      # XP curve, level + rank math
    │   │   │   ├── DailyReset.kt       # streak helpers
    │   │   │   └── Rewards.kt          # achievement evaluator
    │   │   └── ui/
    │   │       ├── LevelUpApp.kt       # nav host + bottom bar
    │   │       ├── LevelUpViewModel.kt
    │   │       ├── components/         # shared visual atoms
    │   │       ├── screens/            # Dashboard, Missions, Detail, Create, Forge, Profile, Settings
    │   │       └── theme/Theme.kt
    │   └── res/                        # colors, strings, icons
    └── test/java/com/levelup/app/domain/ # unit tests for Progression + DailyReset
```

## AI Quest Forge

By default the Forge runs offline and uses a keyword-driven template generator. To use a real model:

1. Open **System → AI Quest Generator** and pick **OpenAI**.
2. Paste an `sk-...` key. It is stored in DataStore on your device.
3. Optionally change the model (default `gpt-4o-mini`).

If the API call fails for any reason (no network, bad key, rate limit), the Forge falls back to offline suggestions automatically. The app **never** sends data anywhere unless AI mode is on and you tap *Forge*.

## Backup

`System → Backup → Export` writes a `levelup-backup.json` file using the Android Storage Access Framework — you choose where it goes. *Import* reads any `levelup-backup.json` you point it at and restores the snapshot, replacing the current data.

## Roadmap (post-v0.1)

- Notifications + daily quest reminders
- Mission categories / tags
- Charts for XP-per-day and stat trends
- Optional cloud sync
- Widgets for the daily mission list

## Disclaimer

LevelUp is inspired by the RPG-progression *idea* found in stories like *Solo Leveling*, but does not use any copyrighted names, characters, art, or trademarks from those properties.
