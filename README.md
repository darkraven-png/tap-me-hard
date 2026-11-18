# TapMeHard (tap-me-hard)

A modern, minimal Android utility application built in Kotlin with Jetpack Compose (Material 3) that emulates a customizable hardware "Back Tap" gesture system using device sensors.

## Features

- **Rear Physical Tap Detection**:
  - Leverages hardware `Sensor.TYPE_ACCELEROMETER` (Z-axis) to capture physical finger shockwaves on the back of the device.
  - High-pass filtering eliminates low-frequency device movement, tilting, and walking.
  - Gyroscope rejection prevents false triggers from device rotation.
  - 150ms vibration cooldown prevents bounce echoes from a single hit.
  - Resetting evaluation timer (default 500ms) waits for sequence completion.
- **Morse Code Sequence Engine**:
  - Encodes physical back taps into Morse code format:
    - Normal tap = `.` (Dot)
    - Firm/hard tap = `-` (Dash)
  - Strict sequence guard ensures all automations adhere to Morse code (`.` and `-`).
- **Lock Screen & Background Persistence**:
  - Persistent Foreground Service (`FOREGROUND_SERVICE_SPECIAL_USE`) alongside an Android `AccessibilityService`.
  - Automatically pauses when the screen turns off (`ACTION_SCREEN_OFF`) and resumes on `ACTION_SCREEN_ON` (including on the lock screen).
- **Automations**:
  - Toggle flashlight (torch)
  - Take screenshot (via Accessibility Service)
  - Play / Pause media playback
  - Launch any installed app (with keyguard dismiss support)
- **User Feedback**:
  - System toast and in-app dashboard feedback for matched taps (e.g., `"Double Dot tap detected, opening Chrome app"`).
  - Wrong tap detection alert and error haptic pulse for unconfigured sequences.
- **Lightweight Storage**:
  - Preferences DataStore paired with Kotlinx Serialization (JSON) — zero Room / SQLite overhead.

## Architecture

- **UI**: 100% Jetpack Compose (Material 3), Single Activity (`MainActivity`).
- **State Management**: AndroidViewModel, Kotlin Coroutines, StateFlow / SharedFlow.
- **Target SDK**: 34 (Android 14) / **Min SDK**: 28 (Android 9 Pie).

## Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/darkraven-png/tap-me-hard.git
   ```
2. Open in Android Studio (Hedgehog 2023.1 or newer).
3. Build and deploy to a physical Android device (accelerometer required).
4. Grant required system permissions via the in-app permission status cards.

## License

MIT License
