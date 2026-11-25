package com.simplekioskbrowser.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists [KioskSettings] in SharedPreferences (MODE_PRIVATE, app-private).
 *
 * The [SharedPreferences] is injected so the class is straightforward to unit test without an
 * Android [Context]. Use [fromContext] to build an instance from an Activity/Application.
 */
class SettingsRepository(private val prefs: SharedPreferences) {

    companion object {
        fun fromContext(context: Context): SettingsRepository =
            SettingsRepository(context.getSharedPreferences("kiosk_settings", Context.MODE_PRIVATE))
    }

    // Keys must remain stable; changing them would erase saved configuration.
    private val URL = "url"
    private val RELOAD_CONNECTIVITY = "reload_connectivity"
    private val RELOAD_WEBVIEW_ERROR = "reload_webview_error"
    private val PERIODIC_ENABLED = "periodic_enabled"
    private val PERIODIC_INTERVAL = "periodic_interval"
    private val PERIODIC_UNIT = "periodic_unit"
    private val BATTERY_THRESHOLD = "battery_threshold"
    private val BATTERY_ACTION = "battery_action"

    /** Returns the currently persisted settings (defaults if never saved). */
    fun getSettings(): KioskSettings {
        return KioskSettings(
            url = prefs.getString(URL, null),
            reloadOnConnectivity = prefs.getBoolean(RELOAD_CONNECTIVITY, false),
            reloadOnWebViewError = prefs.getBoolean(RELOAD_WEBVIEW_ERROR, false),
            periodicReloadEnabled = prefs.getBoolean(PERIODIC_ENABLED, false),
            periodicInterval = prefs.getInt(PERIODIC_INTERVAL, 30).coerceIn(1, 10_080),
            periodicIntervalUnit = prefs.getString(PERIODIC_UNIT, "minutes").let { raw ->
                val v = raw ?: "minutes"
                if (SettingsValidation.isValidIntervalUnit(v)) v else "minutes"
            },
            batteryThreshold = prefs.getInt(BATTERY_THRESHOLD, 20).coerceIn(0, 100),
            batteryAction = prefs.getString(BATTERY_ACTION, BatteryAction.DIM).let { raw ->
                val v = raw ?: BatteryAction.DIM
                if (SettingsValidation.isValidBatteryAction(v)) v else BatteryAction.DIM
            }
        )
    }

    /** Persists [settings] after validating it. Returns false if validation fails. */
    fun saveSettings(settings: KioskSettings): Boolean {
        if (!isValid(settings)) return false
        prefs.edit()
            .putString(URL, settings.url)
            .putBoolean(RELOAD_CONNECTIVITY, settings.reloadOnConnectivity)
            .putBoolean(RELOAD_WEBVIEW_ERROR, settings.reloadOnWebViewError)
            .putBoolean(PERIODIC_ENABLED, settings.periodicReloadEnabled)
            .putInt(PERIODIC_INTERVAL, settings.periodicInterval)
            .putString(PERIODIC_UNIT, settings.periodicIntervalUnit)
            .putInt(BATTERY_THRESHOLD, settings.batteryThreshold)
            .putString(BATTERY_ACTION, settings.batteryAction)
            .apply() // asynchronous; sufficient for settings persistence
        return true
    }

    /** Returns true when [settings] passes all validation rules. */
    fun isValid(settings: KioskSettings): Boolean {
        return SettingsValidation.isValidUrl(settings.url) &&
            SettingsValidation.isValidInterval(settings.periodicInterval) &&
            SettingsValidation.isValidIntervalUnit(settings.periodicIntervalUnit) &&
            SettingsValidation.isValidBatteryThreshold(settings.batteryThreshold) &&
            SettingsValidation.isValidBatteryAction(settings.batteryAction)
    }

    /** Convenience: validate and save a URL, keeping the other fields unchanged. */
    fun saveUrl(url: String): Boolean {
        val current = getSettings()
        val updated = current.copy(url = url.trim().takeIf { it.isNotEmpty() })
        return if (updated.url == null) false else saveSettings(updated)
    }

    fun clearUrl() {
        prefs.edit().putString(URL, null).apply()
    }
}
