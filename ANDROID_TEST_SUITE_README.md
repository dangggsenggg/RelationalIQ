# Android Test Suite Setup (Workspace Local)

This workspace now includes a full local Android testing toolchain under `.tools`.

## Installed Components

- Temurin JDK 17 (`.tools/jdk`)
- Android Command-line Tools (`.tools/android-sdk/cmdline-tools/latest`)
- Platform Tools (adb)
- Emulator
- Android SDK Platform 35
- Android Build-Tools 35.0.0
- Android SDK Platform 34
- Android Build-Tools 34.0.0
- System image: `system-images;android-35;google_apis;x86_64`
- AVD: `Pixel_6_API_35`

## Scripts Added

- `activate-android-test-env.ps1`: Sets `JAVA_HOME`, `ANDROID_SDK_ROOT`, and `PATH` for the current shell.
- `verify-android-test-suite.ps1`: Verifies all required SDK test components are installed.
- `run-android-tests.ps1`: Boots emulator (optional) and runs:
  - `gradlew.bat testDebugUnitTest`
  - `gradlew.bat connectedDebugAndroidTest`

## Usage

PowerShell on this machine uses `AllSigned`, so run scripts with process-level bypass:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
```

Activate environment:

```powershell
.\activate-android-test-env.ps1
```

Verify setup:

```powershell
.\verify-android-test-suite.ps1
```

Run tests once your Android project exists:

```powershell
.\run-android-tests.ps1 -ProjectDir "path-to-your-android-project"
```

Skip emulator boot if you already have a device/emulator connected:

```powershell
.\run-android-tests.ps1 -ProjectDir "path-to-your-android-project" -SkipEmulator
```
