# Software Requirements Specification (SRS)

## 1. Purpose and scope
Define an MVP single-page Android kiosk browser. The app is intended to show a user-configured URL with minimal controls, fullscreen presentation, and automatic recovery.

## 2. Functional requirements
- **FR-01 URL configuration:** Settings allow an operator to enter, validate, save, and launch a URL. Support HTTP and HTTPS; HTTPS should be recommended. Reject malformed/empty input with a clear error.
- **FR-02 Web display:** Display the saved URL in an Android WebView occupying the primary screen.
- **FR-03 Fullscreen:** Hide system bars using supported immersive APIs, reapply immersive presentation when appropriate, and handle system gestures without claiming absolute OS lockdown.
- **FR-04 Keep awake:** Keep the display awake while the kiosk screen is foregrounded using supported activity-level mechanisms; release behavior must be correct when app leaves foreground.
- **FR-05 Hidden settings gesture:** A translucent gear icon handle in the top-right corner opens the settings panel. It expands for 10 seconds on app open, then collapses to a small tab (or fully hides in "hide after 10s" mode). Settings panel provides configuration and a return-to-page action. The handle must not interfere excessively with page interactions.
- **FR-06 Network recovery:** When network connectivity is lost, optionally show a discreet offline/recovery state. When connectivity returns, reload or retry the configured page if enabled. Avoid repeated reload loops.
- **FR-07 Web failure recovery:** Optionally retry/reload on WebView network-related errors. Do not treat every HTTP response (including legitimate site error pages) as a network disconnect unless explicitly defined.
- **FR-08 Timed reload:** Enable/disable periodic refresh and configure a valid interval and unit. Default interval should be conservative; recommend 5 minutes as a default only if enabled. Refresh must be lifecycle-aware and must not accumulate timers.
- **FR-09 Battery threshold:** Configurable battery threshold, default 20%, and configurable action. MVP must evaluate feasibility of screen-off/lock behavior on the actual device. If true screen-off is not permitted, report constraints and implement only a safe, explicitly described fallback (such as a user-visible pause) rather than claiming the screen is powered off.
- **FR-10 Persistence:** Save settings locally across app restarts.
- **FR-11 Initial setup:** If no URL is configured, show settings/setup rather than loading a blank or fabricated URL.
- **FR-12 Recovery and state:** On app restart, restore saved settings and load configured URL. Handle WebView renderer/process loss gracefully where possible.

## 3. Non-functional requirements
- **NFR-01 Simplicity:** Single-purpose application; no server, account, analytics, or cloud dependency.
- **NFR-02 Reliability:** Avoid crashes, runaway retries, duplicate timers/callbacks, and blank-screen dead ends.
- **NFR-03 Responsiveness:** Settings panel and recovery UI remain responsive; WebView operations are main-thread-safe.
- **NFR-04 Security:** Do not disable TLS validation. Do not enable file access, universal access, or JavaScript bridge APIs unless a justified requirement exists. Use least-privilege permissions.
- **NFR-05 Privacy:** No telemetry or transmission of settings except web navigation itself.
- **NFR-06 Compatibility:** Support the test device's Android version and document minimum/target SDK rationale.
- **NFR-07 Maintainability:** Clear separation between UI, WebView handling, settings persistence, network state, and battery logic.
- **NFR-08 Battery:** Timers and monitoring must be lifecycle-aware and avoid unnecessary background execution. Screen-on mode itself consumes battery.

## 4. Workflows
### First run
1. Launch app.
2. If no URL is saved, reveal/display setup settings.
3. Enter URL and relevant toggles/interval/threshold.
4. Save and load the site.
5. Settings can be reopened by tapping the translucent gear icon handle in the top-right corner.

### Normal operation
1. Saved URL opens fullscreen.
2. Display remains awake while the activity is in the foreground (unless low-battery policy intervenes).
3. Optional timed reload operates without stacking timers.
4. Settings gesture opens panel and settings can be changed and saved.

### Connectivity interruption
1. Detect loss of network using system network state.
2. Optionally present an unobtrusive offline indication.
3. On restoration and/or relevant WebView failure, reload according to settings.
4. Debounce retries and prevent infinite reload loops.

### Low battery
1. Read current battery level.
2. When threshold condition is met, perform the configured feasible action.
3. Explain and test device-policy constraints; recover/resume according to documented behavior when battery rises or charging occurs.

## 5. Inputs and outputs
**Inputs:** URL, reload-on-connect toggle, timed-refresh toggle, interval and unit, low-battery threshold and action, network/battery/system events, touch gesture.

**Outputs:** rendered web page, settings panel, loading/offline/error states, page reloads, and configured low-battery response.

## 6. Business rules and validation
- URL must be non-empty and syntactically valid, with `http://` or `https://`; do not silently rewrite user-entered hostnames in a surprising way.
- Interval must be positive and within a documented sensible range; guard against overflow and excessively frequent refresh.
- Battery threshold must be 0–100.
- Reload triggers must be debounced/coalesced.
- Offline status is not equivalent to every page error; preserve distinctions between network, SSL, HTTP, and app/WebView errors.
- Settings changes should be applied predictably; saving a new URL should load it.

## 7. Security
- Use only required Android permissions (`INTERNET` and network-state access as justified).
- Never bypass SSL certificate errors.
- Avoid exposing JavaScript interfaces to arbitrary page content.
- Restrict file/content access in WebView unless specifically justified and documented.
- Do not include credentials or secrets in logs.
- Settings handle is obscurity, not authentication. Agent may recommend optional PIN protection, but must not add it as a mandatory requirement without explaining.

## 8. Error handling
- Invalid URL or interval: show validation feedback and retain previous valid configuration.
- WebView load failures: distinguish recoverable connectivity errors; offer retry and automatic recovery.
- Network callbacks: handle registration/unregistration across lifecycle.
- WebView renderer loss: recreate or recover view safely.
- Battery/policy action unavailable: show/document limitation and safe fallback.
- ADB/build/device errors: report actual evidence, not assumed success.

## 9. Acceptance criteria
- AC-01 App builds successfully with reproducible documented commands.
- AC-02 First run guides user to URL setup.
- AC-03 Valid saved URL renders in WebView.
- AC-04 Fullscreen presentation is applied and survives relevant UI/lifecycle transitions.
- AC-05 Keep-awake behavior works while app is foregrounded.
- AC-06 Translucent settings handle reliably opens settings; return control restores page.
- AC-07 Settings persist across process/app restart.
- AC-08 Network loss/restoration recovery is demonstrated or documented with device test evidence.
- AC-09 Timed reload works at selected interval and can be disabled; no duplicate timers.
- AC-10 Battery threshold is validated and threshold response is tested or its device-policy limitation documented.
- AC-11 No SSL bypass, unjustified dangerous permissions, or insecure WebView settings.
- AC-12 Tests cover URL validation, preference logic, trigger debouncing, and applicable component behavior.
- AC-13 Final report identifies untested scenarios, build/device facts, and limitations.
