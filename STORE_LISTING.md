# App Store Listing — Simple Android Kiosk Browser

## Short description (≤80 characters)
Single-site fullscreen kiosk browser with auto-recovery.

## Full description (≤4000 characters)

Simple Android Kiosk Browser turns any Android phone or tablet into a dedicated, always-on web display. Configure a single website, and the app opens it fullscreen, and recovers automatically from connectivity hiccups and page staleness. Settings are hidden behind a setting icon that can be completely hidden.

**Features**

- Configurable URL with HTTP/HTTPS validation.
- Fullscreen immersive display; system bars hidden as far as the OS allows.
- Screen stays awake while the kiosk activity is foregrounded.
- Automatic recovery: reload on connectivity restoration and on recoverable WebView network errors, with debounce and loop protection.
- Optional periodic page refresh on a configurable interval and unit (minutes/hours), lifecycle-aware (paused when not foregrounded, no duplicate timers).
- Settings persist across app restarts.
- Graceful handling of loading, offline, invalid URL, SSL, HTTP, and renderer-failure states.
- Setting icon can be completely hidden.

**Privacy**

- No analytics, telemetry, accounts, or cloud services.
- Settings are stored locally on the device only.
- Only INTERNET and ACCESS_NETWORK_STATE permissions.
- Open source: https://github.com/ni6hant/FullscreenKioskBrowser_Android

**Notes**

- This is a single-purpose kiosk browser, not a general-purpose browser. There is no address bar, tabs, or bookmarks.
- True device lockdown (blocking the home button, etc.) requires device-owner/DPC provisioning and is out of scope for this app.
- HTTPS is recommended; HTTP is permitted for local/intranet kiosk servers.

**Open source**

The complete source code is available on GitHub: https://github.com/ni6hant/FullscreenKioskBrowser_Android
You are free to audit, fork, and modify it.
