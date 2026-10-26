# CLAUDE.md

Guidance for Claude when working in this repository.

## What this is

**Corny Task** — a small personal Android app (built for one user, my wife) that turns everyday life into a gamified task list: complete TODOs and time goals to earn coins, spend coins on rewards. It also has notes, mood tracking, a calendar, a menstrual cycle tracker, reminders and a home-screen widget.

- Package / applicationId: `com.nobadhabbits.cornytask`
- Single module: `:app` (root project name is still `cornytask-v2`)
- Kotlin + Jetpack Compose (Material 3), minSdk 26, target/compileSdk 36, JVM target 11
- Default branch: `master` (GitHub: `mtricolici98/cornytask-native`)

Keep things simple. This is a hobby app, not a product. Prefer small, readable changes that match the existing style over new abstractions or frameworks.

## Build & run

Windows dev machine (Android Studio). From the repo root:

```bash
./gradlew assembleDebug        # gradlew.bat on Windows cmd
./gradlew installDebug         # install on a connected device/emulator
./gradlew build                # what CI runs
./gradlew test                 # unit tests (placeholder only, see below)
./gradlew connectedAndroidTest # instrumented tests (need a device)
```

- Dependency versions live partly in `gradle/libs.versions.toml` and partly as inline strings in `app/build.gradle.kts`. When adding a dependency, prefer the version catalog.
- KSP is used for the Room compiler.
- CI: `.github/workflows/android.yml` runs `./gradlew build` on push/PR to `master` with JDK 21. Toolchain: Gradle 9, AGP 9 (built-in Kotlin, no separate `kotlin-android` plugin), Kotlin 2.4, KSP 2.3. Builds fine on Android Studio's bundled JDK.
- To release, bump `versionCode` and `versionName` in `app/build.gradle.kts`. Release builds are signed with a local key (`CORNYKEY`) and output to `app/release/`. Never commit keystores or passwords.

## Architecture

```
app/src/main/java/com/nobadhabbits/cornytask/
├── App.kt                 # Application; creates AppContainer
├── MainActivity.kt        # Launcher: routes to Login or MainMenu, enqueues notification worker
├── di/AppContainer.kt     # Manual DI (currently only the Room cycle DB/repo)
├── data/                  # Firestore data classes (Todo, Reward, History, TimeGoal, Note, User, MoodTrackRecord)
│   └── cycle/             # Room entity/DAO/DB/repository for the cycle tracker (local only)
├── features/<feature>/    # One package per feature: *Screen.kt, *ViewModel.kt, *Repository.kt, factories
├── services/              # FirebaseMessagingService (stores FCM token on the user doc)
└── ui/theme/              # Compose theme (Color, Type, Shapes, Theme)
```

**Pattern:** Compose screen → `ViewModel` (exposes `StateFlow` via `stateIn(..., WhileSubscribed(5000), ...)`) → Repository. Repositories are created directly inside ViewModels (no Hilt/Koin). ViewModels that need a `Context` get a hand-written `*ViewModelFactory`.

**Navigation:** `features/main/MainMenuActivity.kt` hosts the `NavHost` with a bottom bar. Routes are defined in `features/main/Screen.kt` (`Screen`, `MoreScreen`, `Routes`).
- Bottom tabs: TODOs, Rewards, Time Goals (nested graph with `timer_screen`), More (nested graph: History, Notes, Mood, Settings).
- Inside `TodoScreen`, internal tabs switch between the TODO list, `CalendarScreen` and `CycleScreen`. These are not nav routes.
- Add/Edit TODO are separate Activities (`AddTodoActivity`, `EditTodoActivity`), not Compose destinations.

## Data & backend

- **Auth:** Credential Manager (`androidx.credentials` + `googleid`) gets a Google ID token in `LoginActivity` → Firebase Auth. Firebase is the only "logged in?" check (`AuthSession.isSignedIn()`); `MainMenuActivity` listens to auth state and returns to login if the session drops. Sign out via `AuthSession.signOut` (stops timer, clears widget + credential state, signs out of Firebase).
- **Firestore** (main store), everything scoped per user:
  `users/{uid}` (coins, fcmToken, active time goal) with subcollections `todos`, `rewards`, `history`, `timeGoals`, `notes`, `moods`.
- Repositories expose Firestore data as `callbackFlow`s that listen to `FirebaseAuth` state and re-attach snapshot listeners on login/logout. Follow this pattern for new collections, and always remove listeners in `awaitClose`.
- Firestore boolean fields use `@get:/@set:PropertyName("isX")` to avoid the `is` prefix mapping bug. Keep that annotation on any new `isSomething` field.
- `UserRepository.getUserFlow()` creates the user document on first login.
- Completing a TODO writes a `History` entry in a batch and adjusts coins via `UserRepository.addCoins/spendCoins`.
- **MedAT plan** (`features/medat/`): the studymed 111-day plan is bundled as `assets/medat_plan.json` (generated from the PDF; item ids like `d12-3` are stored in Firestore, so don't renumber them). Progress lives in `users/{uid}/medat`, one doc per touched item (`isDone`, `day` when postponed, linked `timeGoalId`/`todoId`, `rewardCoins`/`historyId` when ticked by hand), plus a `_calendar` doc mapping dates to BREAK/SIMULATION. Ticking an item by hand pays 5 unicorns/hour (`medatCoinsFor`); `TimeGoal`/`Todo` carry `medatItemIds` and completing them marks those items done without paying twice. The test date is `MEDAT_DATE` and the finish target `MEDAT_TARGET_DATE` (a month earlier), both in `MedatPlan.kt`. The plan JSON was generated by a throwaway script; BMS new-topic items are ⌈180 / topics that day⌉ minutes.
- **Room** (`cycle_db`) is used only for cycle tracking and uses `fallbackToDestructiveMigration()`. Bumping the DB version wipes local cycle data. Add a real migration before changing `CycleEntry` if the data matters.
- **SharedPreferences** `time_goal_prefs` holds active timer state (`TimeGoalPreferences`).
- `google-services.json` lives at `app/src/google-services.json` and is gitignored (not tracked). Don't duplicate or move it without checking the Firebase setup.

## Background work, notifications, widget

Most of this lives in `features/widget/` (despite the name, it covers all reminders):
- `NotificationScheduler`: exact `AlarmManager` alarms for TODO due dates, plus daily mood and drink-water reminders.
- Receivers: `TodoNotificationReceiver`, `MoodNotificationReceiver`, `DrinkSomeWaterReceiver`, `BootCompletedReceiver` (re-schedules after reboot).
- `OneTimeNotificationSchedulerWorker` is enqueued from `MainActivity` (unique work, `KEEP`).
- **Glance widget:** `TodoWidget`, `TodoWidgetReceiver`, `TodoWidgetWorker` (periodic, 15 min) and `TodoWidgetActions`. After any TODO change in the app, send the `ACTION_DATA_UPDATED` broadcast to `TodoWidgetReceiver` so the widget refreshes (see `TodoViewModel.sendDataUpdatedBroadcast`).
- `features/time_goals/TimeGoalService`: foreground service (`dataSync` type) for the running timer.
- Assistant / deep link: `app://cornytask/createTodo?query=...` → `CreateTodoFromAssistantActivity` (see `res/xml/shortcuts.xml`).

New receivers, services and activities must be registered in `AndroidManifest.xml`.

## Conventions

- Kotlin official code style (`kotlin.code.style=official`), 4-space indent.
- New features go in `features/<feature_name>/` as Screen + ViewModel + Repository. Shared Firestore models go in `data/`.
- UI is Compose + Material 3. Use the theme in `ui/theme/` rather than hard-coded colors. Icons come from `material-icons-extended`.
- Coroutines/Flow everywhere. Use `kotlinx-coroutines-play-services` `.await()` for Firebase tasks in suspend functions.
- User-facing strings are mostly inline right now. That's fine for this app; don't do a big string-resource migration unless asked.

## Testing

Only the generated placeholder tests exist (`ExampleUnitTest`, `ExampleInstrumentedTest`, still under the old `com.example.cornytask_v2` package). There's no meaningful test suite. Verify changes by building (`./gradlew assembleDebug`) and, where possible, running on a device. If you add logic that's easy to unit-test (for example `CyclePredictor`), adding a JUnit test is welcome.

## Gotchas

- Release artifacts (`app/release/…`, including unpacked `.aab` contents) and `.kotlin/errors/*.log` are committed. Don't add more build outputs to git, and don't edit those files.
- `FirebaseMessagingService` only stores the token; incoming messages are logged, not shown.
- `isMinifyEnabled = false` for release, so the ProGuard rules are unused.
