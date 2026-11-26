# Fullscreen Kiosk Browser

> **Declaration:** This initial APK release was generated with the assistance of an LLM (large language model). The author plans to manually review and modify the code going forward without LLM assistance.

A lightweight Android app that displays one configurable website in a fullscreen WebView, keeps the display awake during kiosk operation, and automatically recovers from common connectivity and page-staleness conditions. Settings are accessed through a translucent gear icon handle in the top-right corner.

## Device used for verification
- Xiaomi M2101K6P (Redmi Note 10), LineageOS
- Android 16 / API 36
- ADB serial `ab082808`, authorized over USB

## Build

Requires the Android SDK (`C:\Android` on the dev machine) and Java 21. A `local.properties` file pointing `sdk.dir` at your SDK is needed (it is gitignored and machine-specific).

```powershell
# Windows PowerShell
.\gradlew.bat assembleDebug
```

or with the system Gradle:

```powershell
gradle assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`

## Install and run on a device

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.simplekioskbrowser/.MainActivity
```

## First-run setup

On first launch, if no URL is saved, the settings panel opens. Enter a valid `http://` or `https://` URL, then tap **SAVE**. The page loads fullscreen and stays awake. Open settings anytime by tapping the translucent gear icon in the top-right corner; tap \*\*RETURN TO PAGE\*\* to close settings.

## Settings

| Setting | Meaning |
|---|---|
| Website URL | The single URL loaded by the kiosk (http/https only). |
| Reload when connectivity returns | Reload the page when network connectivity is restored (debounced). |
| Retry on page load failure | Retry on recoverable WebView network errors (capped to avoid loops). |
| Periodic page refresh | Enable automatic periodic reloads. |
| Refresh interval | Positive number + unit (minutes/hours), lifecycle-aware (paused when not foregrounded). |
| Auto-reload cooldown (seconds) | 5-3600, default 30. Minimum spacing between automatic reloads (connectivity/WebView errors) to prevent reload loops. |
| Pinch to zoom (two fingers) | Enable/disable pinch-to-zoom on the page. |
| Clear cache on every refresh | Clear the WebView cache before each reload. |
| Hide settings button after 10 seconds | Completely hide the settings handle after 10s (still shows for 10s on every app start). Handle turns red in this mode. |
| Low battery threshold (%) | 0–100, default 20%. |
| Low battery action | `None` or `Pause refreshes`. |
| Forced orientation | `Auto`, `Landscape`, `Portrait`, `Reverse Landscape`, `Reverse Portrait`. |

## Security

- Only `INTERNET` and `ACCESS_NETWORK_STATE` permissions.
- WebView: JavaScript enabled for modern-site compatibility (documented trade-off); no file access, no JavaScript bridges, no universal access, safe browsing enabled where available.
- Non-HTTP(S) URI schemes are blocked in the WebView.
- SSL certificate errors are never bypassed: `onReceivedSslError` cancels the handler and shows a clear error.
- No analytics, telemetry, cloud backend, or external data transmission beyond normal web navigation.
- No credentials or secrets are logged.

## Battery policy limitations (device-policy feasibility)

A normal app cannot power off or lock the screen without Device Administrator / device-owner provisioning or vendor-specific APIs. This app intentionally does not provision device-owner status. The low-battery options are:
- **Pause refreshes** — pauses periodic reloads and shows a notice.
- **None** — no action.

Brightness is always controlled by the system; this app never dims or powers off the screen.

The app disclaims this on the settings screen: *"This app cannot power off or lock the screen without device-owner/DPC provisioning."*

## Testing

```powershell
.\gradlew.bat testDebugUnitTest
```

37 unit tests cover URL validation, settings validation/persistence, reload debounce/coalescing, auto-attempt capping, periodic timer lifecycle (no duplicates), pause/resume, and battery threshold logic.

## Known limitations

- True kiosk lock-down (preventing OS navigation/home) requires device-owner/DPC provisioning and is out of scope.
- Immersive fullscreen is best-effort and can be interrupted by system gestures/policy.
- Screen-on consumes battery while kiosk is active.
- The app does not manipulate battery level or system provisioning for testing; low-battery behaviour is unit-tested via pure logic.
