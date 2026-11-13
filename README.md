# FFI Trainer Pro

**FFI Trainer Pro** is an independent, 100% offline native Android training simulator built with Kotlin, Jetpack Compose, Material Design 3, Room, and DataStore Preferences.

> **Independence & Security Notice:** FFI Trainer Pro is a standalone simulator. It never interacts with Free Fire India or any third-party game, requests zero network or overlay permissions, and stores all training data locally on-device.

## Features

1. **Practice Arena (`PRACTICE`)**
   - Multi-touch Jetpack Compose `Canvas` arena with virtual joystick, drag-aim control area, and simulated FIRE action button.
   - Moving geometric targets bounded within the arena, Euclidean hit & center-zone detection, per-target reaction-time tracking, live accuracy calculation, timer, and session reset.
2. **Movement Trainer (`TRAINER -> MOVEMENT`)**
   - Virtual joystick with adjustable dead zone, sensitivity, acceleration, nonlinear response curve visualizer, and drag-aim sensitivity.
   - Five movement drills: Left-Right Strafe, Circular Orbit, Controlled Crouch Sim, Jump Timing Sim, and Drag-Aim Practice.
   - Three independently persisted Room profiles: `RUSH`, `DEFENSIVE`, and `SNIPER`.
3. **Aim & Reaction Trainer (`TRAINER -> AIM & REACT`)**
   - Four difficulty tiers: `EASY`, `MEDIUM`, `HARD`, `PRO`.
   - Tracks total attempts, hits, misses, overall accuracy, center-zone accuracy, average reaction time (`ms`), flick accuracy, and total score.
4. **Sensitivity Calibrator (`TRAINER -> CALIBRATE`)**
   - Presets (`LOW`, `MEDIUM`, `HIGH`, `CUSTOM`), live drag preview pad, guided 6-target calibration challenge, calibration score, and simulator-derived sensitivity recommendations with explicit independence notice.
5. **Performance Analytics (`STATISTICS`)**
   - Real statistics computed from Room `TrainingSession` records (zero fake data), empty-state handling, custom Canvas accuracy & reaction-time trend charts, session history, CSV export via `FileProvider`, and shareable plain-text summary.
6. **Settings & Security (`SETTINGS`)**
   - DataStore preferences for haptic feedback, synthesized offline sound effects, practice duration (30s / 45s / 60s), crosshair reticle styles, HUD grid overlay, default reset, and Privacy Declaration.

## Architecture

- **Pattern:** MVVM + Repository Pattern (Single Activity, Single Module)
- **Package / Application ID:** `com.ffitrainerpro.app`
- **Min SDK / Target SDK:** 26 / 34 (`compileSdk` 36)
- **Persistence:** Room (`training_sessions`, `user_profiles`, `sensitivity_settings`) + DataStore Preferences
- **Navigation:** Jetpack Navigation Compose with `@Serializable` type-safe routes

## Build & Test

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```
