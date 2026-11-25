package com.simplekioskbrowser.battery

import android.app.Activity
import android.view.WindowManager.LayoutParams
import com.simplekioskbrowser.settings.BatteryAction

/** Pure: true when the battery is at/below [threshold] and the device is not plugged in. */
fun isLowBattery(info: BatteryInfo, threshold: Int): Boolean =
    info.percentage >= 0 && info.percentage <= threshold && !info.plugged

/**
 * Evaluates the battery threshold and applies the configured feasible action.
 *
 * Device-policy limitation (FR-08 / ARCHITECTURE.md): a normal app cannot power off or lock the
 * screen without device-owner/DPC provisioning. This policy therefore implements safe fallbacks
 * only:
 *  - "dim": lowers this app window's brightness (NOT a true screen power-off).
 *  - "pause": signals the UI to pause refreshes and show a low-battery notice.
 *  - "none": no action.
 *
 * When the battery rises above the threshold or the device is plugged in, the previous state is
 * restored.
 */
class LowBatteryPolicy(
    private val activity: Activity,
    private var threshold: Int = 20,
    private var action: String = BatteryAction.DIM
) {

    private var currentlyLow = false
    var onActivated: (() -> Unit)? = null
    var onCleared: (() -> Unit)? = null

    fun configure(threshold: Int, action: String) {
        this.threshold = threshold.coerceIn(0, 100)
        this.action = action
    }

    fun evaluate(info: BatteryInfo) {
        val low = isLowBattery(info, threshold)
        if (low && !currentlyLow) {
            applyLowBattery()
            currentlyLow = true
        } else if (!low && currentlyLow) {
            restore()
            currentlyLow = false
        }
    }

    private fun applyLowBattery() {
        when (action) {
            BatteryAction.DIM -> dimScreen(true)
            BatteryAction.PAUSE -> onActivated?.invoke()
            BatteryAction.NONE -> onActivated?.invoke()
        }
    }

    private fun restore() {
        when (action) {
            BatteryAction.DIM -> dimScreen(false)
            else -> onCleared?.invoke()
        }
    }

    private fun dimScreen(dim: Boolean) {
        val wp = activity.window.attributes
        wp.screenBrightness = if (dim) 0.2f else LayoutParams.BRIGHTNESS_OVERRIDE_OFF
        activity.window.attributes = wp
    }
}
