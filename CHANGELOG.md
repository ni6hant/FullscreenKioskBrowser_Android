# Changelog

## [1.1] - 2026-11-26

### What's New
- **New settings access method**: Replaced the left-edge swipe gesture with a translucent gear icon handle in the top-right corner. It expands for 10 seconds on app start, then collapses to a small tab. Tap the visible tab to open settings.
- **Hide settings button option**: New toggle (off by default) to completely hide the settings handle after 10 seconds. Handle turns red in this mode. Still shows for 10 seconds on every app start.
- **Forced orientation**: Choose Auto, Landscape, Portrait, Reverse Landscape, or Reverse Portrait.
- **Pinch-to-zoom**: Two-finger zoom on the page (toggleable, on by default).
- **Clear cache on refresh**: Optional toggle to clear WebView cache before each reload.
- **Configurable auto-reload cooldown**: Default 30 seconds (was 5s) to prevent frequent reload loops on bad URLs. Range 5-3600 seconds.
- **HTTP cleartext support**: HTTP URLs now load correctly (was blocked with ERR_CLEARTEXT_NOT_PERMITTED).
- **Removed screen-dimming mode**: Brightness is now controlled by the system.

### Improvements
- Settings panel background reverted to opaque white for better readability.
- Settings handle is now translucent light blue (#60ADD8E6).
- App renamed to "Fullscreen Kiosk Browse" (Play Store 30-char limit).
- Package name changed to com.ni6hant.fullscreen_kiosk_browser.
- minSdk lowered to 21 (Android 5.0) for broader device support.
- 40+ unit tests covering URL validation, settings persistence, reload debounce/coalescing, periodic timer lifecycle, and battery threshold logic.

### Bug Fixes
- Fixed crash on page load (NullPointerException on null favicon).
- Fixed ERR_CLEARTEXT_NOT_PERMITTED for HTTP URLs.
- Fixed settings panel not accessible with gesture navigation enabled.
- Fixed settings handle completely hiding (now shows a small tab).

### Known Limitations
- True kiosk lock-down (blocking home button) requires device-owner/DPC provisioning.
- Immersive fullscreen is best-effort and can be interrupted by system gestures.
- Low-battery screen-off requires device-owner/DPC; only Pause refreshes / None actions available.
- Settings handle is fully hidden in "hide after 10s" mode — restart app to access settings.

## [1.0] - Initial Release
- Basic kiosk browser with WebView, fullscreen, keep-awake.
- First-run URL setup, URL validation, persistent settings.
- Left-edge swipe to open settings (removed in 1.1).
- Network recovery and periodic refresh.
- Low-battery threshold (default 20%).
- No settings button or swipe gesture in initial release.
