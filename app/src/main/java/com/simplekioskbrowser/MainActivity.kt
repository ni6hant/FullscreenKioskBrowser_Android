package com.simplekioskbrowser

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ArrayAdapter
import com.simplekioskbrowser.databinding.ActivityMainBinding
import com.simplekioskbrowser.battery.BatteryInfo
import com.simplekioskbrowser.battery.BatteryMonitor
import com.simplekioskbrowser.battery.LowBatteryPolicy
import com.simplekioskbrowser.network.ConnectivityMonitor
import com.simplekioskbrowser.reload.PeriodicReloadScheduler
import com.simplekioskbrowser.reload.ReloadCoordinator
import com.simplekioskbrowser.reload.ReloadReason
import com.simplekioskbrowser.settings.BatteryAction
import com.simplekioskbrowser.settings.KioskSettings
import com.simplekioskbrowser.settings.OrientationLock
import com.simplekioskbrowser.settings.SettingsRepository
import com.simplekioskbrowser.settings.SettingsValidation
import com.simplekioskbrowser.webview.WebViewController

/**
 * Main kiosk activity. Hosts the WebView, applies immersive fullscreen and keep-awake, and
 * coordinates settings, connectivity recovery, periodic refresh, and low-battery policy.
 */
class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webViewController: WebViewController
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var reloadCoordinator: ReloadCoordinator
    private lateinit var periodicScheduler: PeriodicReloadScheduler
    private lateinit var connectivityMonitor: ConnectivityMonitor
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var lowBatteryPolicy: LowBatteryPolicy

    private var panelWidth = 0
    private var settingsPanelOpen = false
    private var lastBatteryInfo = BatteryInfo(-1, -1, 0, false)

    private var reloadOnConnectivity = false
    private var reloadOnWebViewError = false
    private var hideSettingsAfterTimeout = false

    private val handleHandler = Handler(Looper.getMainLooper())
    private var handleCollapseRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep the display awake while this activity is foregrounded (FR-04). The flag is tied to
        // the window and is released when the activity is destroyed.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyImmersiveMode()

        // --- WebView ---
        webViewController = WebViewController(this, ::shouldReloadOnWebViewError)
        webViewController.bind(binding.webview)
        webViewController.onReloadRequested = {
            reloadCoordinator.scheduleReload(ReloadReason.MANUAL, isAuto = false)
        }
        webViewController.onUrlLoadFinished = {
            reloadCoordinator.resetAutoAttempts()
        }

        // --- Settings repository ---
        settingsRepository = SettingsRepository.fromContext(this)

        // --- Reload coordination (debounced, loop-protected) ---
        reloadCoordinator = ReloadCoordinator(onReload = { webViewController.reload() })

        // --- Periodic refresh (lifecycle-aware, single timer) ---
        periodicScheduler = PeriodicReloadScheduler(onReload = {
            reloadCoordinator.scheduleReload(ReloadReason.PERIODIC, isAuto = false)
        })

        // --- Battery monitoring + policy ---
        batteryMonitor = BatteryMonitor(this)
        lowBatteryPolicy = LowBatteryPolicy(this)
        lowBatteryPolicy.onActivated = {
            showLowBatteryNotice()
            periodicScheduler.pause()
        }
        lowBatteryPolicy.onCleared = {
            hideLowBatteryNotice()
            periodicScheduler.resume()
        }

        // --- Connectivity monitoring ---
        connectivityMonitor = ConnectivityMonitor(context = this,
            onAvailable = {
                reloadCoordinator.resetAutoAttempts()
                val settings = settingsRepository.getSettings()
                if (settings.reloadOnConnectivity) {
                    reloadCoordinator.scheduleReload(
                        ReloadReason.CONNECTIVITY_RESTORED,
                        isAuto = true
                    )
                }
            },
            onLost = {
                // Connectivity lost; the WebView will reflect this on the next load attempt.
            }
        )

        // --- Settings hover handle (top-right) ---
        binding.settingsScrim.setOnClickListener { closeSettingsPanel() }
        binding.settingsHandle.setOnClickListener { openSettingsPanel() }
        binding.settingsHandle.post {
            expandSettingsHandle()
            scheduleHandleCollapse()
        }

        // --- Settings form actions ---
        binding.settingsSave.setOnClickListener { onSaveSettings() }
        binding.settingsReturn.setOnClickListener { closeSettingsPanel() }

        // Spinner adapters
        val unitAdapter = ArrayAdapter.createFromResource(
            this, R.array.interval_units, android.R.layout.simple_spinner_item
        )
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.settingsIntervalUnit.adapter = unitAdapter

        val actionAdapter = ArrayAdapter.createFromResource(
            this, R.array.battery_actions, android.R.layout.simple_spinner_item
        )
        actionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.settingsBatteryAction.adapter = actionAdapter

        val orientationAdapter = ArrayAdapter.createFromResource(
            this, R.array.orientation_options, android.R.layout.simple_spinner_item
        )
        orientationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.settingsOrientation.adapter = orientationAdapter

        // Settings panel starts off-screen to the left.
        binding.settingsPanel.post {
            panelWidth = binding.settingsPanel.width
            binding.settingsPanel.translationX = -panelWidth.toFloat()
        }

        // Restore configuration and, if a URL is already saved, load it; otherwise show setup.
        val saved = settingsRepository.getSettings()
        applySettings(saved)
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
        webViewController.onResume()
        periodicScheduler.resume()
        connectivityMonitor.start()
        batteryMonitor.start { info ->
            lastBatteryInfo = info
            lowBatteryPolicy.evaluate(info)
        }

        // Re-expand the settings handle for 10 seconds on (re)open.
        binding.settingsHandle.post {
            expandSettingsHandle()
            scheduleHandleCollapse()
        }
    }

    override fun onPause() {
        super.onPause()
        webViewController.onPause()
        periodicScheduler.pause()
        connectivityMonitor.stop()
        batteryMonitor.stop()
        handleCollapseRunnable?.let { handleHandler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        webViewController.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode()
    }

    // ------------------------------------------------------------------
    // Fullscreen / immersive (FR-03). Best-effort; system gestures can interrupt it.
    // ------------------------------------------------------------------
    private fun applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_SWIPE
            }
        } else {
            val decor = window.decorView
            decor.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    // ------------------------------------------------------------------
    // Settings panel (FR-05, FR-10)
    // ------------------------------------------------------------------
    private fun toggleSettingsPanel() {
        if (settingsPanelOpen) closeSettingsPanel() else openSettingsPanel()
    }

    private fun openSettingsPanel() {
        populateForm(settingsRepository.getSettings())
        binding.settingsPanel.visibility = View.VISIBLE
        binding.settingsScrim.visibility = View.VISIBLE
        binding.settingsPanel.animate().translationX(0f).setDuration(250).start()
        settingsPanelOpen = true
    }

    private fun closeSettingsPanel() {
        binding.settingsPanel.animate()
            .translationX(-panelWidth.toFloat())
            .setDuration(250)
            .withEndAction {
                binding.settingsPanel.visibility = View.GONE
                binding.settingsScrim.visibility = View.GONE
            }
            .start()
        settingsPanelOpen = false
    }

    // --- Settings hover handle (top-right) ---
    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun expandSettingsHandle() {
        binding.settingsHandle.animate().cancel()
        binding.settingsHandle.translationX = 0f
    }

    private fun collapseSettingsHandle() {
        binding.settingsHandle.animate().cancel()
        if (hideSettingsAfterTimeout) {
            // Completely hide the handle off-screen.
            val fullOffset = dpToPx(56)
            binding.settingsHandle.animate()
                .translationX(fullOffset.toFloat())
                .setDuration(300)
                .start()
        } else {
            // Show only a 16dp tab.
            val collapsedOffset = dpToPx(56 - 16)
            binding.settingsHandle.animate()
                .translationX(collapsedOffset.toFloat())
                .setDuration(300)
                .start()
        }
    }

    private fun scheduleHandleCollapse() {
        handleCollapseRunnable?.let { handleHandler.removeCallbacks(it) }
        val r = Runnable { collapseSettingsHandle() }
        handleCollapseRunnable = r
        handleHandler.postDelayed(r, 10_000)
    }

    private fun populateForm(settings: KioskSettings) {
        binding.settingsUrl.setText(settings.url ?: "")
        binding.settingsReloadConnectivity.isChecked = settings.reloadOnConnectivity
        binding.settingsReloadWebviewError.isChecked = settings.reloadOnWebViewError
        binding.settingsPeriodicEnabled.isChecked = settings.periodicReloadEnabled
        binding.settingsInterval.setText(settings.periodicInterval.toString())
        binding.settingsBatteryThreshold.setText(settings.batteryThreshold.toString())
        binding.settingsIntervalUnit.setSelection(if (settings.periodicIntervalUnit == "hours") 1 else 0)
        binding.settingsBatteryAction.setSelection(when (settings.batteryAction) {
            BatteryAction.PAUSE -> 1
            else -> 0
        })
        binding.settingsOrientation.setSelection(when (settings.orientationLock) {
            OrientationLock.LANDSCAPE -> 1
            OrientationLock.PORTRAIT -> 2
            OrientationLock.REVERSE_LANDSCAPE -> 3
            OrientationLock.REVERSE_PORTRAIT -> 4
            else -> 0
        })
        binding.settingsAutoReloadInterval.setText(settings.autoReloadIntervalSeconds.toString())
        binding.settingsPinchZoom.isChecked = settings.pinchZoomEnabled
        binding.settingsClearCache.isChecked = settings.clearCacheOnRefresh
        binding.settingsHideAfterTimeout.isChecked = settings.hideSettingsAfterTimeout
    }

    private fun readSettings(): KioskSettings {
        val intervalText = binding.settingsInterval.text.toString().trim()
        val interval = intervalText.toIntOrNull()?.coerceIn(1, 10_080) ?: 30
        val unit = when (binding.settingsIntervalUnit.selectedItem.toString()) {
            "Hours" -> "hours"
            else -> "minutes"
        }
        val thresholdText = binding.settingsBatteryThreshold.text.toString().trim()
        val threshold = thresholdText.toIntOrNull()?.coerceIn(0, 100) ?: 20
        val action = when (binding.settingsBatteryAction.selectedItem.toString()) {
            "Pause refreshes" -> BatteryAction.PAUSE
            else -> BatteryAction.NONE
        }
        val orientation = when (binding.settingsOrientation.selectedItem.toString()) {
            "Landscape" -> OrientationLock.LANDSCAPE
            "Portrait" -> OrientationLock.PORTRAIT
            "Reverse Landscape" -> OrientationLock.REVERSE_LANDSCAPE
            "Reverse Portrait" -> OrientationLock.REVERSE_PORTRAIT
            else -> OrientationLock.AUTO
        }
        val autoReloadSeconds = binding.settingsAutoReloadInterval.text.toString().trim().toIntOrNull()
            ?.coerceIn(5, 3600) ?: 30
        return KioskSettings(
            url = binding.settingsUrl.text.toString().trim().takeIf { it.isNotEmpty() },
            reloadOnConnectivity = binding.settingsReloadConnectivity.isChecked,
            reloadOnWebViewError = binding.settingsReloadWebviewError.isChecked,
            periodicReloadEnabled = binding.settingsPeriodicEnabled.isChecked,
            periodicInterval = interval,
            periodicIntervalUnit = unit,
            batteryThreshold = threshold,
            batteryAction = action,
            orientationLock = orientation,
            autoReloadIntervalSeconds = autoReloadSeconds,
            pinchZoomEnabled = binding.settingsPinchZoom.isChecked,
            clearCacheOnRefresh = binding.settingsClearCache.isChecked,
            hideSettingsAfterTimeout = binding.settingsHideAfterTimeout.isChecked
        )
    }

    private fun onSaveSettings() {
        val settings = readSettings()
        if (!settingsRepository.isValid(settings)) {
            binding.settingsUrlError.visibility = View.VISIBLE
            return
        }
        binding.settingsUrlError.visibility = View.GONE
        settingsRepository.saveSettings(settings)
        applySettings(settings)
        closeSettingsPanel()
    }

    // ------------------------------------------------------------------
    // Apply persisted settings: reconfigures WebView toggles, scheduler, battery policy,
    // and loads the URL (or shows first-run setup if no URL).
    // ------------------------------------------------------------------
    private fun applySettings(settings: KioskSettings) {
        reloadOnConnectivity = settings.reloadOnConnectivity
        reloadOnWebViewError = settings.reloadOnWebViewError

        // Configurable auto-reload cooldown (default 30s) to avoid frequent reloads on bad URLs.
        reloadCoordinator.configureMinInterval(settings.autoReloadIntervalSeconds * 1000L)

        // Pinch-to-zoom and clear-cache-on-refresh toggles.
        webViewController.pinchZoomEnabled = settings.pinchZoomEnabled
        webViewController.clearCacheOnRefresh = settings.clearCacheOnRefresh

        hideSettingsAfterTimeout = settings.hideSettingsAfterTimeout
        // Update handle color: red when hide mode is on.
        val iconColor = if (hideSettingsAfterTimeout) android.graphics.Color.RED else android.graphics.Color.WHITE
        (binding.settingsHandle.getChildAt(0) as? android.widget.ImageView)?.setColorFilter(iconColor)

        // Re-trigger the 10-second collapse with the new hide-mode setting.
        binding.settingsHandle.post {
            expandSettingsHandle()
            scheduleHandleCollapse()
        }

        // Forced orientation (FR optional: Auto/Landscape/Portrait/Reverse Landscape/Reverse Portrait)
        requestedOrientation = when (settings.orientationLock) {
            OrientationLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            OrientationLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationLock.REVERSE_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            OrientationLock.REVERSE_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        val intervalMinutes = SettingsValidation.intervalToMinutes(
            settings.periodicInterval, settings.periodicIntervalUnit
        )
        periodicScheduler.configure(
            settings.periodicReloadEnabled,
            (intervalMinutes.toLong() * 60 * 1000)
        )

        lowBatteryPolicy.configure(settings.batteryThreshold, settings.batteryAction)
        lowBatteryPolicy.evaluate(lastBatteryInfo)

        if (SettingsValidation.isValidUrl(settings.url)) {
            webViewController.loadUrl(settings.url!!)
        } else {
            // First run (FR-11): no valid URL configured, so show setup instead of a blank page.
            openSettingsPanel()
        }
    }

    // ------------------------------------------------------------------
    // Low-battery policy UI
    // ------------------------------------------------------------------
    private fun showLowBatteryNotice() {
        binding.lowBatteryNotice.visibility = View.VISIBLE
        binding.lowBatteryNotice.bringToFront()
    }

    private fun hideLowBatteryNotice() {
        binding.lowBatteryNotice.visibility = View.GONE
    }

    // Honoured by the WebView client to decide whether a WebView network error triggers a reload.
    fun shouldReloadOnWebViewError(): Boolean = reloadOnWebViewError
    fun shouldReloadOnConnectivity(): Boolean = reloadOnConnectivity
}
