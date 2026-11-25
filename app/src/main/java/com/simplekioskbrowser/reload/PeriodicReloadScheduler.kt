package com.simplekioskbrowser.reload

import android.os.Handler
import android.os.Looper

/**
 * Lifecycle-aware periodic page refresh. Uses a single one-shot timer that reschedules itself,
 * so there is never more than one pending timer (no duplicate timers). Paused when the activity
 * is not foregrounded.
 *
 * The [scheduler] parameter is injectable so tests can drive time deterministically.
 */
class PeriodicReloadScheduler(
    private val onReload: () -> Unit,
    private val scheduler: ReloadScheduler? = null,
) {

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val schedule: ReloadScheduler = scheduler ?: { task, delay ->
        handler.postDelayed(task, delay)
        val cancel: () -> Unit = { handler.removeCallbacks(task) }
        cancel
    }

    private var enabled = false
    private var intervalMillis: Long = 0
    private var pendingCancel: (() -> Unit)? = null

    fun configure(enabled: Boolean, intervalMillis: Long) {
        this.enabled = enabled
        this.intervalMillis = intervalMillis.coerceAtLeast(1)
        cancel()
        if (this.enabled) scheduleNext()
    }

    fun pause() {
        cancel()
    }

    fun resume() {
        if (enabled) scheduleNext()
    }

    fun isEnabled(): Boolean = enabled

    private fun scheduleNext() {
        val cancel = schedule({
            onReload()
            if (enabled) scheduleNext() // maintain the loop
        }, intervalMillis)
        pendingCancel = cancel
    }

    private fun cancel() {
        pendingCancel?.invoke()
        pendingCancel = null
    }
}
