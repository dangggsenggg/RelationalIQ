$ErrorActionPreference = "Stop"

Set-Location -Path $PSScriptRoot

$jdkRoot = Join-Path $PSScriptRoot ".tools\jdk"
$sdkRoot = Join-Path $PSScriptRoot ".tools\android-sdk"

if (-not (Test-Path $jdkRoot)) {
    throw "JDK folder not found at $jdkRoot"
}

if (-not (Test-Path $sdkRoot)) {
    throw "Android SDK folder not found at $sdkRoot"
}

$jdkDir = (Get-ChildItem $jdkRoot -Directory | Select-Object -First 1)
if (-not $jdkDir) {
    throw "No JDK installation found under $jdkRoot"
}

$env:JAVA_HOME = $jdkDir.FullName
$env:ANDROID_SDK_ROOT = (Resolve-Path $sdkRoot).Path
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT

$pathsToAdd = @(
    (Join-Path $env:JAVA_HOME "bin"),
    (Join-Path $env:ANDROID_SDK_ROOT "cmdline-tools\latest\bin"),
    (Join-Path $env:ANDROID_SDK_ROOT "platform-tools"),
    (Join-Path $env:ANDROID_SDK_ROOT "emulator")
)

$env:Path = (($pathsToAdd -join ";") + ";" + $env:Path)

Write-Host "Android test environment activated for this terminal session."
Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "ANDROID_SDK_ROOT=$env:ANDROID_SDK_ROOT"

java -version
adb version
sdkmanager.bat --version
