package com.simplekioskbrowser.settings

/**
 * Persisted kiosk configuration. All fields have safe defaults so the repository can
 * always return a fully-populated object even when nothing has been saved yet.
 *
 * @param url The configured website URL (null until first-run setup is complete).
 * @param reloadOnConnectivity Reload the page when network connectivity is restored.
 * @param reloadOnWebViewError Retry/reload when the WebView reports a recoverable
 *     network-related load failure.
 * @param periodicReloadEnabled Whether periodic auto-refresh is enabled.
 * @param periodicInterval The refresh interval magnitude (positive).
 * @param periodicIntervalUnit "minutes" or "hours".
 * @param batteryThreshold Low-battery threshold, 0-100 percent (default 20).
 * @param batteryAction One of [BatteryAction].
 */
data class KioskSettings(
    val url: String? = null,
    val reloadOnConnectivity: Boolean = false,
    val reloadOnWebViewError: Boolean = false,
    val periodicReloadEnabled: Boolean = false,
    val periodicInterval: Int = 30,
    val periodicIntervalUnit: String = "minutes",
    val batteryThreshold: Int = 20,
    val batteryAction: String = BatteryAction.DIM
)

object BatteryAction {
    const val NONE = "none"
    const val DIM = "dim"
    const val PAUSE = "pause"
}

/**
 * Validation helpers for settings. Pure functions: no Android dependencies, easy to unit test.
 */
object SettingsValidation {
    // URL must be http:// or https:// and parse as a URL. Intranet hosts without a TLD are
    // accepted as long as the scheme is valid, because a kiosk may point at a local server.
    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)) {
            return false
        }
        return try {
            val parsed = java.net.URL(trimmed)
            if (parsed.host.isNullOrBlank()) return false
            parsed.toURI()
            true
        } catch (_: Exception) {
            false
        }
    }

    // Interval magnitude must be positive and within a sane bound to avoid excessive refresh.
    fun isValidInterval(interval: Int): Boolean = interval in 1..10_080 // up to 7 days

    fun isValidIntervalUnit(unit: String): Boolean = unit == "minutes" || unit == "hours"

    fun isValidBatteryThreshold(threshold: Int): Boolean = threshold in 0..100

    fun isValidBatteryAction(action: String): Boolean =
        action == BatteryAction.NONE || action == BatteryAction.DIM || action == BatteryAction.PAUSE

    /** Normalises an interval into minutes regardless of the configured unit. */
    fun intervalToMinutes(interval: Int, unit: String): Int = when (unit) {
        "hours" -> interval * 60
        else -> interval
    }
}
