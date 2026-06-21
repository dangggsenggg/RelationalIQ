param(
    [string]$ProjectDir = ".",
    [string]$AvdName = "Pixel_6_API_35",
    [switch]$SkipEmulator
)

$ErrorActionPreference = "Stop"

Set-Location -Path $PSScriptRoot
. "$PSScriptRoot\activate-android-test-env.ps1"

$projectPath = Resolve-Path $ProjectDir
$gradlew = Join-Path $projectPath "gradlew.bat"

if (-not (Test-Path $gradlew)) {
    throw "No gradlew.bat found in $projectPath. Point -ProjectDir to your Android project root."
}

if (-not $SkipEmulator) {
    $existing = adb devices | Select-String "emulator-"
    if (-not $existing) {
        Write-Host "Starting emulator $AvdName ..."
        Start-Process -FilePath (Join-Path $env:ANDROID_SDK_ROOT "emulator\emulator.exe") -ArgumentList "-avd $AvdName -no-snapshot-save -netdelay none -netspeed full" | Out-Null
    }

    Write-Host "Waiting for emulator to boot ..."
    adb wait-for-device

    $booted = $false
    for ($i = 0; $i -lt 120; $i++) {
        $status = (adb shell getprop sys.boot_completed 2>$null).Trim()
        if ($status -eq "1") {
            $booted = $true
            break
        }
        Start-Sleep -Seconds 2
    }

    if (-not $booted) {
        throw "Emulator did not fully boot in time."
    }
}

Push-Location $projectPath
try {
    & $gradlew testDebugUnitTest
    & $gradlew connectedDebugAndroidTest
}
finally {
    Pop-Location
}

Write-Host "Android unit and instrumentation tests completed."
