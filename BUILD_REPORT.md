# Build Report — FFI Trainer Pro

- **Application Name:** FFI Trainer Pro
- **Application ID / Package:** `com.ffitrainerpro.app` (source namespace `com.example`)
- **Version:** `1.0.0` (versionCode `1`)
- **Min SDK / Target SDK / Compile SDK:** `26` / `34` / `36`
- **Build System:** Gradle Kotlin DSL + KSP

## Compilation & APK Generation

- `compile_applet` (`assembleDebug`): **SUCCEEDED**
- Generated Debug APK path inside workspace: `app/build/outputs/apk/debug/app-debug.apk`
- Release/APK export availability in AI Studio: Users can export the full project ZIP, push to GitHub, or generate downloadable APK/AAB builds directly via the **Settings menu** in Google AI Studio.

## Automated Local Verification (`:app:testDebugUnitTest`)

- **Status:** **PASSED** (`8/8` tests passed, `0` failures, `0` errors)
- `TrainingEngineUnitTest` (4 tests):
  - Joystick dead-zone filtering & response curve scaling
  - Target outer-ring vs center-zone bullseye hit detection
  - Calibration overshoot recommendation calculation
  - Empty & populated statistics calculation and CSV export formatting
- `ExampleRobolectricTest` (3 tests, Robolectric SDK 34):
  - `app_name` string verification (`FFI Trainer Pro`)
  - Room in-memory database profile seeding (`RUSH`, `DEFENSIVE`, `SNIPER`) and `sessionKey` duplicate prevention
  - Compose UI navigation across all 4 tabs (`PRACTICE`, `TRAINER`, `STATISTICS`, `SETTINGS`), Trainer sub-tabs, and Settings Privacy Declaration
- `ExampleUnitTest` (1 test): passed

## Streaming Emulator / ADB Note

- Direct ADB or interactive Android Emulator UI execution is unavailable in the server-side build agent container; visual and live touch verification on the streaming emulator occurs when the user interacts with the deployed preview after turn completion.
