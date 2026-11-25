# Master Agentic Development Prompt — Simple Android Kiosk Browser

You are the autonomous implementation agent for **Simple Android Kiosk Browser**. Implement the complete MVP in the workspace. Do not stop after analysis or planning: proceed through implementation, testing, fixing, and verification, pausing only for genuinely critical decisions, credentials, destructive operations, or actions that require explicit user authorization.

## 1. Source documents and hierarchy
Read and reconcile all supplied project documents before coding:
- `README.md`
- `SRS.md`
- `ARCHITECTURE.md`
- `IMPLEMENTATION.md`

Treat the SRS as the requirement baseline and architecture/plan as guidance. Where documents conflict, flag the conflict and resolve using the source brief, safety, and documented assumptions. Preserve scope; do not add unnecessary features.

## 2. Workspace and environment inspection
- Inspect the entire workspace and repository before modifying anything.
- If Git exists, inspect `git status`, recent history, branches, and relevant files. Preserve all existing user work and unrelated changes.
- If no Git repository exists, initialize a local repository with `git init`. Keep it private/local; do not configure any remote.
- Inspect the Windows environment, Android SDK, Gradle, JDK, and available build tooling. Use Windows-compatible commands and PowerShell syntax where appropriate. Do not assume a Unix shell.
- Inspect ADB availability and run `adb devices -l`. The supplied device serial was `ab082808`, but verify before using it. Record Android version, SDK level, manufacturer/model, and current authorization state.
- Do not change unrelated device settings, wipe data, provision device owner, enable debugging beyond what is already authorized, or perform destructive actions without approval.

## 3. Requirements
Implement:
1. Native Android single-page kiosk browser using Kotlin and Android WebView.
2. First-run setup that requires a valid URL before loading.
3. URL validation for HTTP/HTTPS and persistent local settings.
4. Fullscreen/immersive display using supported APIs, handling interruptions and reapplying mode where appropriate.
5. Keep-screen-on behavior while the kiosk activity is foregrounded.
6. Settings access via swipe inward from the left edge, with a settings panel and return-to-page behavior. Gesture must not unnecessarily interfere with page touch/scroll.
7. Optional automatic reload/retry on connectivity restoration and recoverable WebView network errors, with debounce/coalescing and no reload loops.
8. Optional periodic page refresh using a configurable positive interval and unit, with lifecycle-aware scheduling and no duplicate timers.
9. Low-battery threshold setting (default 20% unless the UI makes it configurable), and the specified low-battery response. Investigate device-policy feasibility. True screen-off/lock may require Device Admin/device-owner permission or vendor-specific support; do not claim a black screen/dim UI is a true screen power-off. Use a safe, explicitly described fallback if necessary. Do not provision device-owner status or grant privileged access without explicit approval.
10. Restore configuration and behavior after app restart.
11. Graceful loading, offline, invalid URL, SSL, HTTP, WebView process/renderer failure, and other error states.
12. Clear separation of settings, UI, WebView, network monitoring, reload coordination, battery policy, and tests, appropriate to project scale.

## 4. Security and reliability
- Never bypass TLS/SSL certificate validation or accept SSL errors.
- Use only required permissions.
- Avoid unnecessary JavaScript bridges, file access, universal access, or other unsafe WebView settings. Enable JavaScript only when necessary for target-site compatibility and document why.
- Handle non-HTTP(S) URI schemes safely.
- Do not log secrets or page credentials.
- Avoid analytics, telemetry, cloud services, backend, or external data transmission.
- Ensure timers/callbacks are disposed or unregistered appropriately and do not leak across lifecycle events.
- Avoid excessive refresh rates, battery consumption, overengineering, or needless dependencies.

## 5. Implementation process
Work sequentially, adapting milestone boundaries to the project:
- Repository/environment reconnaissance and baseline.
- Scaffold/configuration/preferences.
- WebView and fullscreen/keep-awake.
- Settings gesture and persistence.
- Connectivity recovery and periodic refresh.
- Battery threshold policy and fallback.
- Testing, security review, device verification, documentation, packaging.

At every stage, make decisions based on current Android guidance and actual environment. If choosing an alternative to the recommended stack, explain and document why.

## 6. Mandatory local Git checkpointing
Keep all work in a private local Git repository throughout implementation.

### Repository safeguards
- Reuse an existing Git repository; do not blindly reinitialize it.
- If there is no repository, run `git init`.
- Never configure a remote or connect to GitHub, GitLab, Bitbucket, or any hosting service.
- Never log in, authenticate, push, publish, upload, or transmit project files or Git history externally.
- Inspect existing status/history before changes and preserve existing modifications.
- Do not commit unrelated changes.

### Checkpoints
Create meaningful commits at completed milestones, adapted to actual work, such as:
1. Initial structure/configuration/docs.
2. Foundational architecture/preferences.
3. WebView and fullscreen.
4. Settings and gesture.
5. Network/reload/battery features.
6. Tests and bug fixes.
7. Final verified implementation and documentation.

### Commit hygiene
- Use descriptive commit messages and logically group changes.
- Ensure the working tree is understood before committing.
- Maintain an appropriate `.gitignore`.
- Exclude secrets, credentials, API keys, passwords, private certificates, local configuration, build outputs, dependency caches, temporary files, and unrelated changes.
- Do not run destructive commands such as `git reset --hard`, `git clean -fd`, or history-rewriting commands without explicit authorization.
- If commit fails, diagnose and report; do not silently skip.
- Never claim a commit exists unless Git confirms it.

## 7. Testing and verification
- Write meaningful automated unit tests for URL validation, settings validation, interval behavior, battery threshold logic, and reload debounce/coalescing.
- Add instrumentation/UI tests where useful and feasible.
- Build the application and run tests using commands appropriate for the environment. Fix discovered failures and repeat.
- Use the connected Android device through ADB if it remains available and authorized. Confirm its serial before installing. Install, launch, and test actual behavior, not just compilation.
- Verify, as feasible: first-run setup; valid URL; invalid URL; settings gesture and return; settings persistence; fullscreen; keep-awake; timed refresh; offline/reconnection recovery; low-battery policy; app restart; renderer recovery.
- Do not manipulate battery level, system state, or device provisioning unsafely merely to force a test. Use safe simulation/unit tests for unavailable cases and label them.
- Record concrete evidence: commands run, test results, device details, build artifacts, and limitations.
- Do not claim success for functionality that was not verified.

## 8. Documentation and final deliverables
Maintain documentation as implementation changes:
- README with project purpose, build/install/use, settings, and troubleshooting.
- SRS/acceptance criteria updated if justified changes are made.
- Architecture and implementation notes, including security and battery policy limitations.
- Test/verification report with actual results.
- APK output if successfully built, but avoid including generated artifacts or binaries in Git unless explicitly required. APK may be included in final deliverable ZIP as a separate artifact if practical.

Package the finished project in a ZIP **including its local `.git` repository and complete commit history** so milestones can be inspected. Ensure the package excludes secrets, caches, temporary artifacts, and unnecessary binaries; do not package unrelated pre-existing files.

## 9. Completion criteria
The project is complete only when:
- A reproducible build succeeds or a precise environment limitation is reported.
- Automated tests have been run and failures resolved or documented.
- Acceptance criteria have been individually checked with evidence or marked not verified.
- Device testing is performed where available, with actual device facts documented.
- Documentation reflects the delivered implementation.
- Git milestones are committed and repository status/history checked.
- No remote repository is configured and nothing was pushed or uploaded.

## 10. Final report
Provide a concise but detailed report:
- Implemented features and any deviations.
- Build/test commands and results.
- Device used, Android details, and actual device test results.
- Acceptance criteria checklist and unverified items.
- Known bugs, limitations, assumptions, and required user actions.
- Paths to deliverables and APK, if built.
- Whether local Git was initialized or reused, number of commits, milestone commit messages, and working-tree status.
- Explicit confirmation that no remote repository was configured and nothing was pushed or uploaded.

Proceed autonomously and keep the implementation proportional to this small app. Do not stop after writing a plan.
