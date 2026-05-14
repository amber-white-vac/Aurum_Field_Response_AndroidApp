# Aurum Field Response

A native Android field safety app that turns real-time rugged device location and hazard data into map-based incident flags, then routes workers or supervisors to the correct response steps.

## Why It Fits Aurum

Aurum Technologies describes its work around human performance improvement, radiation safety, remote monitoring, location awareness, AI-assisted decision support, and ALARA-minded field workflows. This app is scoped to that mission: it keeps the field map first, converts telemetry into actionable safety flags, and opens the right response workflow without burying the worker in extra screens.

## What Works

- Opens directly to a live operational map.
- Streams synthetic rugged-device tracker movement for three field users.
- Classifies risky conditions from telemetry:
  - critical dose-rate alarm
  - controlled-boundary breach
  - lost device signal
  - elevated dose-rate advisory
- Creates a color-coded map flag for each incident.
- Sends a local push-style Android notification when a new flag is created.
- Shows an in-app alert at the bottom of the map.
- Tapping a map flag selects the matching sidebar workflow.
- Keeps synthetic data isolated behind `TrackerSource` so real telemetry can replace it cleanly.

## Architecture

- `domain`: production-style models and `HazardClassifier`
- `data`: `TrackerSource`, synthetic tracker stream, and repository state assembly
- `ui`: Compose `ViewModel` state for readings, flags, alerts, and workflows
- `MainActivity`: notification channel, permission request, and map-first Compose UI

The map rendering is an in-app operational map surface so the demo builds without API keys. A production map SDK can replace the surface while leaving the tracker source, classifier, repository, and workflow routing intact.

## Build

Use Android Studio's JDK 17 or any JDK 17+:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```
