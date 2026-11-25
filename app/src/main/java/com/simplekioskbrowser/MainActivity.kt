package com.simplekioskbrowser

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Spinner
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
import com.simplekioskbrowser.settings.SettingsRepository
import com.simplekioskbrowser.settings.SettingsValidation
import com.simplekioskbrowser.ui.EdgeSwipeView
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

        // --- Hidden edge gesture ---
        binding.edgeSwipeView.setOnEdgeSwipeListener { toggleSettingsPanel() }
        binding.settingsScrim.setOnClickListener { closeSettingsPanel() }

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
    }

    override fun onPause() {
        super.onPause()
        webViewController.onPause()
        periodicScheduler.pause()
        connectivityMonitor.stop()
        batteryMonitor.stop()
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
        return KioskSettings(
            url = binding.settingsUrl.text.toString().trim().takeIf { it.isNotEmpty() },
            reloadOnConnectivity = binding.settingsReloadConnectivity.isChecked,
            reloadOnWebViewError = binding.settingsReloadWebviewError.isChecked,
            periodicReloadEnabled = binding.settingsPeriodicEnabled.isChecked,
            periodicInterval = interval,
            periodicIntervalUnit = unit,
            batteryThreshold = threshold,
            batteryAction = action
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
