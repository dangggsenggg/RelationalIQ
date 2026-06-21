$ErrorActionPreference = "Stop"

Set-Location -Path $PSScriptRoot

$checks = @(
    ".tools\jdk",
    ".tools\android-sdk\cmdline-tools\latest\bin\sdkmanager.bat",
    ".tools\android-sdk\cmdline-tools\latest\bin\avdmanager.bat",
    ".tools\android-sdk\platform-tools\adb.exe",
    ".tools\android-sdk\emulator\emulator.exe",
    ".tools\android-sdk\platforms\android-35",
    ".tools\android-sdk\build-tools\35.0.0",
    ".tools\android-sdk\system-images\android-35\google_apis\x86_64"
)

$failed = $false
foreach ($p in $checks) {
    if (Test-Path (Join-Path $PSScriptRoot $p)) {
        Write-Host "OK   $p"
    }
    else {
        Write-Host "MISS $p"
        $failed = $true
    }
}

if ($failed) {
    Write-Error "Android test suite is incomplete."
    exit 1
}

Write-Host "Android test suite is installed and ready."
exit 0
