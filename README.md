# Symbol Match Board 🎯

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-757575?style=flat&logo=materialdesign&logoColor=white)](https://m3.material.io)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24%20(Android%207.0)-blue)](https://developer.android.com)

**Spot the pair. Claim the board.**

**Symbol Match Board** is a modern, offline Android board game built with 100% Kotlin, Jetpack Compose, and Material 3 design principles. Unlike traditional memory-flip games, all symbols on the 6×6 board are fully visible from the beginning. Players compete against a smart CPU opponent to spot and claim matching target symbols scattered across the grid.

---

## 🎮 Gameplay & Core Features

* **Visible Board Strategy**: Play on a $6 \times 6$ grid with 36 cells containing 18 matching symbol pairs. 9 distinct symbol types appear 4 times each.
* **Dynamic Target System**: A prompt displays the target symbol to find. Match two instances of the target symbol to capture the pair.
* **Turn Mechanics**: Successfully capturing a pair awards an extra turn! Missing a match passes turn control to the CPU opponent.
* **Distance Bonus Scoring**: While the game winner is decided strictly by total captured pairs, individual match scores include a Manhattan-distance bonus for strategic play.
* **Adaptive AI Opponent**:
  * 🟢 **Easy**: Relaxed opponent with occasional decoy mistakes ($30\%$) and random pair selection.
  * 🟡 **Normal**: Balanced challenge with smart targeting and rare mistakes ($8\%$).
  * 🔴 **Hard**: Flawless strategy with $0\%$ mistake rate, using distance optimization and leftover board evaluation to leave no easy picks for the player.
* **Rich UI & Feedback**:
  * Animated connection lines, pair pulsing, match reveal transitions, and victory celebrations.
  * Customizable sound effects and haptic vibration feedback.
  * Coordinate grid labels (`A–F`, `1–6`) and target highlight outlines.
* **Theme & Customization**:
  * Dark & Light theme options alongside Android 12+ Dynamic Wallpaper Coloring.
  * Confirm restart/leave dialogs to prevent accidental loss of progress.
* **Local Statistics**: Real-time tracking for games played, total wins, best pairs captured, high scores, and win streaks.
* **Privacy Focused & Offline**: Zero network permissions required, no tracking, and 100% playable offline.

---

## 📖 How to Play

1. **Observe the Target**: Look at the active target symbol displayed at the top of the screen (e.g., *STAR*).
2. **Find the Pair**: Locate two cells on the $6 \times 6$ board holding that exact target symbol.
3. **Capture**: Tap both matching cells. If correct, you claim the pair and get another turn!
4. **Beware of Mismatches**: Selecting an incorrect cell or non-matching pair loses your turn to the CPU.
5. **Win the Game**: Claim more pairs than the CPU before the board is completely cleared.

---

## 🏗️ Architecture & Project Structure

The project follows Modern Android Development (MAD) practices and clean architecture principles with unidirectional data flow (UDF):

```
app/src/main/java/com/example/symbolmatchboard/
├── data/                  # Repositories & Data Persistence
│   ├── KeyValueStore.kt         # SharedPreference abstraction
│   ├── SettingsRepository.kt    # User settings state management
│   └── StatisticsRepository.kt  # Game performance & records tracking
│
├── game/                  # Pure Domain Game Logic & AI
│   ├── ai/
│   │   └── SymbolMatchCpuStrategy.kt  # AI difficulty algorithms & scoring
│   ├── logic/
│   │   ├── GameRules.kt               # Scoring, distances, target picking
│   │   ├── SymbolBoardGenerator.kt    # Randomized fair board generation
│   │   └── SymbolMatchGameEngine.kt   # Immutable game state engine
│   └── model/
│       └── GameModels.kt              # Data classes, Enums, Coordinates
│
├── navigation/            # Compose Navigation
│   └── AppNavigation.kt         # NavHost, routes & bottom bar scaffold
│
├── ui/                    # Presentation Layer (Jetpack Compose)
│   ├── components/              # Board, Symbol, Scoreboard & UI components
│   ├── screens/                 # Splash, Home, Game, Result, Settings, About
│   ├── theme/                   # Material 3 Color palette, Typography, Symbols
│   ├── AppViewModel.kt          # Global App & Settings ViewModel
│   └── SymbolMatchViewModel.kt     # Match state management & game loop
│
└── util/                  # Utilities
    ├── FeedbackPlayer.kt        # Sound & Haptics player
    └── GameTimings.kt           # Animation and turn delay configurations
```

---

## 🛠️ Tech Stack & Tooling

* **Language**: [Kotlin 2.2](https://kotlinlang.org)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Components
* **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
* **Architecture**: Jetpack `ViewModel`, `StateFlow`, `Coroutines`, Clean Architecture
* **State & Data**: Reactive Flows & `SharedPreferences` persistence
* **Testing**: JUnit 4 unit test suite covering game logic, CPU strategy, and repository operations
* **Build System**: Gradle Version Catalog (`libs.versions.toml`), AGP 9.4+

---

## ⚡ Getting Started

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1) or newer recommended
* **JDK**: Version 17+
* **Android SDK**: Min SDK 24 (Android 7.0), Target/Compile SDK 37

### Building & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/abdullah-ateeq/Symbol-Match-Board.git
   cd Symbol-Match-Board
   ```

2. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

3. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on connected device / emulator**:
   ```bash
   ./gradlew installDebug
   ```

---

## 🧪 Testing

The repository contains extensive unit tests for core game mechanics:
* `GameEngineTest.kt`: Tests state transitions, selection logic, pair resolution, turn switches, and game over triggers.
* `CpuStrategyTest.kt`: Validates CPU move choices across Easy, Normal, and Hard difficulty levels.
* `GameRulesTest.kt`: Tests Manhattan distance calculation, scoring bonuses, and outcome determination.
* `SettingsRepositoryTest.kt` & `StatisticsTest.kt`: Verifies persistence, record tracking, and streak calculation logic.

---

## 📄 License

```text
Copyright 2026 Abdullah Ateeq

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied.
See the License for the specific language governing permissions and limitations under the License.
```
