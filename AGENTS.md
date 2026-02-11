# Repository Guidelines

## Docs 
- Docs for the current release are located inside /docs/<current_release_version>
- Docs for the older releases can be found inside /docs/archive/

# Development
- Do not make any assumptions. If there is ambiguity in any part, use the questions tool to ask the user for clarification.
- After doing any major changes run the build and tests to verify them.


## Git
- When working on very large features you can commit smaller changes when necessary to avoid big commits.
- Use format `<release_version> <commit_message>` for commit messages. Release version eg. `v0.1`

## Build, Test, and Development Commands
- `./gradlew assembleDebug`: build debug APK.
- `./gradlew installDebug`: install debug build on a connected device/emulator.
- `./gradlew testDebugUnitTest`: run local JVM tests.
- `./gradlew connectedDebugAndroidTest`: run instrumentation tests on a connected device.
- `./gradlew lintDebug`: run Android lint checks.
- `./gradlew clean`: clear build outputs.
- `./gradlew -q tasks --all`: inspect available tasks.

