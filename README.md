# Simple Android Kiosk Browser

## Project introduction
A lightweight Android app that displays one configurable website in a fullscreen WebView, keeps the display awake during kiosk operation, and automatically recovers from common connectivity and page-staleness conditions. Settings are accessed through a discreet edge swipe.

## Problem statement
A phone needs to behave as a dedicated web display rather than a general-purpose browser, with minimal user-facing controls and automatic recovery.

## Objectives
- Open a configurable URL in an immersive, fullscreen browser.
- Keep the screen awake while kiosk mode is active.
- Provide an edge-swipe gesture to reveal settings.
- Recover from loss/restoration of connectivity and periodically refresh the page.
- Offer a low-battery action configurable around a 20% threshold.

## Scope
**Included:** single-site WebView, settings UI, persisted configuration, edge gesture, fullscreen immersive presentation, network-aware recovery, interval reload, battery threshold monitoring while app is active, and device-based testing.

**Not included in MVP:** general browsing UI, multiple tabs, bookmarks, remote management, MDM integration, app-store distribution, content filtering, or guaranteed device lockdown against OS navigation. True Android lock-task kiosk mode requires device-owner/DPC provisioning or an applicable allowlisting arrangement and is not assumed.

## Target user
The owner/operator of a dedicated Android phone or tablet displaying a web page.

## Features
- Configurable URL and basic URL validation.
- Fullscreen display with Android system bars hidden as far as the OS allows.
- `FLAG_KEEP_SCREEN_ON` while the kiosk activity is foregrounded.
- Settings reveal via swipe from the left edge; settings include URL and recovery/power options.
- Reload on network connectivity restoration, optionally enabled.
- Periodic reload on configurable interval, optionally enabled.
- Reload or retry behavior while offline and when connectivity returns.
- Battery threshold action, default 20%, with explicit disclosure of device-policy constraints.

## High-level architecture
Native Android application in Kotlin, using Android WebView, Android lifecycle APIs, ConnectivityManager network callbacks, BatteryManager/battery broadcasts, and locally persisted preferences. No backend or cloud service is required.

## Assumptions and open questions
- Android version and device model are not yet known; agent must record them using ADB.
- The connected test device is available over ADB and currently reports serial `ab082808` (`adb devices` output supplied by the requester). Verify before targeting it; never assume it is still connected.
- “Close screen if battery less than 20%” means turn the display off or otherwise stop kiosk display activity when the battery falls below the threshold. True power-off/lock-screen control may require Device Administrator/device-owner capabilities; implementation must explain the feasible device-specific behavior and avoid silently substituting a different action. Prefer an explicit configurable action and conservative default.
- “Auto reload on web disconnect” is interpreted as reload/retry when the page reports a load failure or connectivity is restored, with debouncing to avoid reload storms.
- Settings gesture is a left-edge swipe into the screen. Settings should not be accidentally opened by ordinary page scrolling; gesture conflicts need testing.
- A settings PIN is not required by the source brief; consider it a clearly labelled optional enhancement only if warranted.

## How to use the agent prompt
Copy `AGENT_PROMPT.md` into an agentic coding tool operating on the Android project workspace, and supply the accompanying documents. The agent is expected to implement, test, and checkpoint work locally in Git. Do not treat this documentation package as an implementation or as proof that device testing has occurred.
