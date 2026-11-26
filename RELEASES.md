# Releases

Track of all releases published to the Play Store (and other channels).

---

## Release 3 — Open Testing Release

| Field | Value |
|---|---|
| **Release Name** | Open Testing Release |
| **versionCode** | 6 |
| **versionName** | 1.2 |
| **Package** | com.ni6hant.fullscreen_kiosk_browser |
| **Channel** | Open Testing (Play Store) |
| **Date** | 2026-11-26 |
| **Status** | Published |

### Release Notes (v1.2 — Open Testing)

**Open testing release of Fullscreen Kiosk Browser.**

A single-site fullscreen kiosk browser for Android that displays one configurable website in an immersive WebView, keeps the screen awake, and recovers automatically from connectivity and page-staleness conditions.

**Features:**
- Configurable URL with HTTP/HTTPS validation (HTTP permitted for local/intranet servers).
- Fullscreen immersive display with system bars hidden.
- Screen stays awake while the app is foregrounded (`FLAG_KEEP_SCREEN_ON`).
- Hidden settings access via a translucent gear icon handle in the top-right corner (expands for 10 seconds on app start, then collapses to a small tab).
- Optional "hide settings button after 10 seconds" mode (handle turns red and fully disappears).
- Automatic recovery: reload on connectivity restoration and on recoverable WebView network errors, with debounce and loop protection (default 30-second cooldown).
- Optional periodic page refresh on a configurable interval and unit (minutes/hours), lifecycle-aware.
- Low-battery threshold (default 20%) with safe fallbacks (pause refreshes or none). Brightness controlled by the system.
- Forced orientation: Auto, Landscape, Portrait, Reverse Landscape, Reverse Portrait.
- Pinch-to-zoom (two fingers) toggle.
- Clear cache on every refresh toggle.
- Settings persist across app restarts.
- Graceful handling of loading, offline, invalid URL, SSL, HTTP, and renderer-failure states.

**Security:**
- Only `INTERNET` and `ACCESS_NETWORK_STATE` permissions.
- WebView: JavaScript enabled for site compatibility; no file access, no JavaScript bridges, no universal access.
- SSL certificate errors are never bypassed.
- No analytics, telemetry, cloud backend, or external data transmission.
- Open source: https://github.com/ni6hant/FullscreenKioskBrowser_Android

**Known limitations:**
- True kiosk lock-down (blocking home button) requires device-owner/DPC provisioning.
- Immersive fullscreen is best-effort.
- Low-battery screen-off requires device-owner/DPC; only Pause refreshes / None actions available.
- Settings handle is fully hidden in "hide after 10s" mode - restart app to access settings.

---

## Release 2 — Closed Testing Release

| Field | Value |
|---|---|
| **Release Name** | Closed Testing Release |
| **versionCode** | 4 |
| **versionName** | 1.1 |
| **Package** | com.ni6hant.fullscreen_kiosk_browser |
| **Channel** | Closed Testing (Play Store) |
| **Date** | 2026-11-26 |
| **Status** | Published |

### Release Notes (v1.1 - Closed Testing)

**Initial release of Fullscreen Kiosk Browser.**

A single-site fullscreen kiosk browser for Android that displays one configurable website in an immersive WebView, keeps the screen awake, and recovers automatically from connectivity and page-staleness conditions.

**Features:**
- Configurable URL with HTTP/HTTPS validation (HTTP permitted for local/intranet servers).
- Fullscreen immersive display with system bars hidden.
- Screen stays awake while the app is foregrounded (`FLAG_KEEP_SCREEN_ON`).
- Hidden settings access via a translucent gear icon handle in the top-right corner (expands for 10 seconds on app start, then collapses to a small tab).
- Optional "hide settings button after 10 seconds" mode (handle turns red and fully disappears).
- Automatic recovery: reload on connectivity restoration and on recoverable WebView network errors, with debounce and loop protection (default 30-second cooldown).
- Optional periodic page refresh on a configurable interval and unit (minutes/hours), lifecycle-aware.
- Low-battery threshold (default 20%) with safe fallbacks (pause refreshes or none). Brightness controlled by the system.
- Forced orientation: Auto, Landscape, Portrait, Reverse Landscape, Reverse Portrait.
- Pinch-to-zoom (two fingers) toggle.
- Clear cache on every refresh toggle.
- Settings persist across app restarts.
- Graceful handling of loading, offline, invalid URL, SSL, HTTP, and renderer-failure states.

**Security:**
- Only `INTERNET` and `ACCESS_NETWORK_STATE` permissions.
- WebView: JavaScript enabled for site compatibility; no file access, no JavaScript bridges, no universal access.
- SSL certificate errors are never bypassed.
- No analytics, telemetry, cloud backend, or external data transmission.
- Open source: https://github.com/ni6hant/FullscreenKioskBrowser_Android

**Known limitations:**
- True kiosk lock-down (blocking home button) requires device-owner/DPC provisioning.
- Immersive fullscreen is best-effort.
- Low-battery screen-off requires device-owner/DPC; only Pause refreshes / None actions available.
- Settings handle is fully hidden in "hide after 10s" mode - restart app to access settings.

---

## Release 1 — Internal Testing Release

| Field | Value |
|---|---|
| **Release Name** | Internal Testing Release |
| **versionCode** | 3 |
| **versionName** | 1.0 |
| **Package** | com.ni6hant.fullscreen_kiosk_browser |
| **Channel** | Internal Testing (Play Store) |
| **Date** | 2026-11-26 |
| **Status** | Published |

### Release Notes (v1.0 - first Play Store release)

**Initial release of Fullscreen Kiosk Browser.**

A single-site fullscreen kiosk browser for Android that displays one configurable website in an immersive WebView, keeps the screen awake, and recovers automatically from connectivity and page-staleness conditions.

**Features:**
- Configurable URL with HTTP/HTTPS validation (HTTP permitted for local/intranet servers).
- Fullscreen immersive display with system bars hidden.
- Screen stays awake while the app is foregrounded (`FLAG_KEEP_SCREEN_ON`).
- Hidden settings access via a translucent gear icon handle in the top-right corner (expands for 10 seconds on app start, then collapses to a small tab).
- Optional "hide settings button after 10 seconds" mode (handle turns red and fully disappears).
- Automatic recovery: reload on connectivity restoration and on recoverable WebView network errors, with debounce and loop protection (default 30-second cooldown).
- Optional periodic page refresh on a configurable interval and unit (minutes/hours), lifecycle-aware.
- Low-battery threshold (default 20%) with safe fallbacks (pause refreshes or none). Brightness controlled by the system.
- Forced orientation: Auto, Landscape, Portrait, Reverse Landscape, Reverse Portrait.
- Pinch-to-zoom (two fingers) toggle.
- Clear cache on every refresh toggle.
- Settings persist across app restarts.
- Graceful handling of loading, offline, invalid URL, SSL, HTTP, and renderer-failure states.

**Security:**
- Only `INTERNET` and `ACCESS_NETWORK_STATE` permissions.
- WebView: JavaScript enabled for site compatibility; no file access, no JavaScript bridges, no universal access.
- SSL certificate errors are never bypassed.
- No analytics, telemetry, cloud backend, or external data transmission.
- Open source: https://github.com/ni6hant/FullscreenKioskBrowser_Android

**Known limitations:**
- True kiosk lock-down (blocking home button) requires device-owner/DPC provisioning.
- Immersive fullscreen is best-effort.
- Low-battery screen-off requires device-owner/DPC; only Pause refreshes / None actions available.
- Settings handle is fully hidden in "hide after 10s" mode - restart app to access settings.

---

<!-- Add new releases above this line -->
