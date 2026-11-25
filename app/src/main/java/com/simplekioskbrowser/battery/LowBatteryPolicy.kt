package com.simplekioskbrowser.battery

import android.app.Activity
import com.simplekioskbrowser.settings.BatteryAction

/**
 * Evaluates the battery threshold and applies the configured feasible action.
 *
 * Device-policy limitation (FR-08 / ARCHITECTURE.md): a normal app cannot power off or lock the
 * screen without device-owner/DPC provisioning. This policy therefore implements safe fallbacks
 * only:
 *  - "pause": signals the UI to pause refreshes and show a low-battery notice.
 *  - "none": no action.
 *
 * Brightness is left to the system: this app does not dim the screen. When the battery rises
 * above the threshold or the device is plugged in, refreshes resume.
 */
class LowBatteryPolicy(
    private val activity: Activity,
    private var threshold: Int = 20,
    private var action: String = BatteryAction.NONE
) {

    private var currentlyLow = false
    var onActivated: (() -> Unit)? = null
    var onCleared: (() -> Unit)? = null

    fun configure(threshold: Int, action: String) {
        this.threshold = threshold.coerceIn(0, 100)
        this.action = if (action == BatteryAction.PAUSE) action else BatteryAction.NONE
    }

    /** Pure: true when the battery is at/below [threshold] and the device is not plugged in. */
    fun isLow(info: BatteryInfo, threshold: Int): Boolean =
        info.percentage >= 0 && info.percentage <= threshold && !info.plugged

    fun evaluate(info: BatteryInfo) {
        val low = isLow(info, threshold)
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
            BatteryAction.PAUSE -> onActivated?.invoke()
            BatteryAction.NONE -> onActivated?.invoke()
        }
    }

    private fun restore() {
        onCleared?.invoke()
    }
}

/** Pure: true when the battery is at/below [threshold] and the device is not plugged in. */
fun isLowBattery(info: BatteryInfo, threshold: Int): Boolean =
    info.percentage >= 0 && info.percentage <= threshold && !info.plugged
