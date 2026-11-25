package com.simplekioskbrowser.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/** Current battery state reported by ACTION_BATTERY_CHANGED. */
data class BatteryInfo(val level: Int, val scale: Int, val percentage: Int, val plugged: Boolean)

/**
 * Reads battery level via ACTION_BATTERY_CHANGED (a sticky broadcast, no special permission).
 * Registered and unregistered by the activity so monitoring is lifecycle-aware.
 */
class BatteryMonitor(private val context: Context) {

    private var listener: ((BatteryInfo) -> Unit)? = null
    private var receiver: android.content.BroadcastReceiver? = null

    fun start(listener: (BatteryInfo) -> Unit) {
        stop()
        this.listener = listener
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        // ACTION_BATTERY_CHANGED is sticky: registering a null receiver returns the current state.
        val current = context.registerReceiver(null, filter)
        if (current != null) deliver(current)
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) deliver(intent)
            }
        }
        this.receiver = receiver
        context.registerReceiver(receiver, filter)
    }

    private fun deliver(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val percentage = if (scale > 0 && level >= 0) level * 100 / scale else 0
        listener?.invoke(BatteryInfo(level, scale, percentage, plugged > 0))
    }

    fun stop() {
        receiver?.let { context.unregisterReceiver(it) }
        receiver = null
    }
}
