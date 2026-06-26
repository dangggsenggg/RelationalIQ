---
name: testing-relationaliq
description: Test the RelationalIQ Android app end-to-end on an emulator. Use when verifying UI flows, assessment quiz, training engine, or settings changes.
---

# Testing RelationalIQ Android App

## Prerequisites
- Android SDK at `~/android-sdk` (or `$ANDROID_HOME`)
- System image: `system-images;android-34;google_apis;x86_64`
- KVM permissions (may need `sudo chmod 666 /dev/kvm` or `sudo gpasswd -a $USER kvm`)

## Setup Steps

1. **Fix KVM permissions** (if needed):
   ```bash
   sudo chmod 666 /dev/kvm
   ```

2. **Install system image and create AVD** (if not cached):
   ```bash
   export ANDROID_HOME=~/android-sdk
   export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH
   yes | sdkmanager "system-images;android-34;google_apis;x86_64" "emulator"
   echo "no" | avdmanager create avd -n test_device -k "system-images;android-34;google_apis;x86_64" -d "pixel_6" --force
   ```

3. **Start emulator**:
   ```bash
   nohup emulator -avd test_device -no-audio -gpu swiftshader_indirect -no-boot-anim -no-snapshot -skin 1080x2400 > /tmp/emulator.log 2>&1 &
   ```

4. **Wait for boot**:
   ```bash
   adb wait-for-device
   # Poll until boot complete
   while [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do sleep 5; done
   ```

5. **Build and install APK**:
   ```bash
   cd ~/repos/RelationalIQ
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

6. **Launch app**:
   ```bash
   adb shell am start -n com.relationaliq.debug/com.relationaliq.presentation.MainActivity
   ```

## Interacting with the Emulator

- Screen resolution is 1080x2400 (Pixel 6 skin)
- Use `adb shell uiautomator dump` to find exact button coordinates
- Use `adb shell input tap X Y` to tap at coordinates
- Use `adb exec-out screencap -p > screenshot.png` for screenshots
- Use `adb shell input keyevent KEYCODE_BACK` for back navigation
- The first tap coordinate that worked for "Next" on onboarding was (540, 2200)

## App Flow (First Launch)

1. **Onboarding** (3 pages: Welcome → Science → How It Works → "Get Started")
2. **Pre-Assessment** (intro screen → 20 multi-premise quiz questions → results with score/100)
3. **Dashboard** (stats: Streak/XP/Stages, Continue Training, View All Stages)
4. **Training** (Start Session → premise cards → YES/NO → feedback → session summary)
5. **Settings** (gear icon top-right: Dark Mode, High Contrast, Sound, Haptics, Reduced Motion)

## Known Issues

- **Stage loading might fail silently**: `StageDataSource.loadStagesFromAssets()` catches all exceptions and returns empty list. If training shows "Stage not found", the issue is likely Gson parsing failing at runtime (possibly R8/ProGuard stripping reflection metadata). Check `app/proguard-rules.pro` for Gson keep rules.
- The assessment quiz uses hardcoded trials in `AssessmentViewModel`, so it works independently of stages.json loading.

## Key Files for Testing

- Assessment trials: `app/src/main/java/com/relationaliq/presentation/screens/assessment/AssessmentViewModel.kt`
- Stage data: `app/src/main/assets/stages.json`
- Stage loader: `app/src/main/java/com/relationaliq/data/datasource/StageDataSource.kt`
- Navigation: `app/src/main/java/com/relationaliq/presentation/navigation/NavGraph.kt`
- Unit tests: `app/src/test/java/com/relationaliq/`

## Devin Secrets Needed

None required for testing. The app runs fully offline on the emulator.
