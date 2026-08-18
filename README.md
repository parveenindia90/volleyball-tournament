# Volleyball Tournament Manager Android App

A native Android application built with Kotlin, Jetpack Compose, Room Database, and MVVM Architecture.

## Features
- **Configurable Target Points**: Custom point limits (default: 15).
- **Round 1 Bye Calculation**: Automatically creates a balanced power-of-two bracket (e.g., 20 teams -> 12 byes in Round 1, 8 play 4 matches -> Round 2 has 16 teams).
- **Live Score Counter**: Simple + / - buttons to record live points.
- **Auto Advancement**: When a match hits target points, winner is recorded; once all matches in a round finish, next round is auto-generated.
- **Champion Crowning**: Final round winner receives the champion banner.
- **Offline Persistence**: Room SQLite database saves state even if app closes.

## How to Open and Run:
1. **Android Studio**: File -> Open -> Select this extracted folder -> Sync Gradle -> Run.
2. **VS Code / IntelliJ**: Open folder -> Ensure JDK 17 & Android SDK configured -> run `./gradlew installDebug` or `gradlew.bat installDebug`.
