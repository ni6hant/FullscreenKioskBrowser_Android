package com.simplekioskbrowser.reload

import android.os.Handler
import android.os.Looper

object ReloadReason {
    const val CONNECTIVITY_RESTORED = "connectivity_restored"
    const val WEBVIEW_NETWORK_ERROR = "webview_network_error"
    const val PERIODIC = "periodic"
    const val MANUAL = "manual"
}

/**
 * A task scheduler abstraction. The default implementation uses an Android Handler on the main
 * looper; tests can inject a fake scheduler to drive time deterministically.
 */
typealias ReloadScheduler = (task: () -> Unit, delayMillis: Long) -> () -> Unit

/**
 * Centralises reload triggers and applies debounce/coalescing plus a minimum interval between
 * reloads so that connectivity fluctuations or repeated WebView errors cannot cause a reload
 * loop (NFR-02, FR-06, FR-07).
 *
 * Automatic triggers (connectivity restoration, WebView network errors) are capped at
 * [maxAutoAttempts] consecutive attempts until a successful load or connectivity event resets
 * the counter. Manual and periodic reloads are not capped.
 */
class ReloadCoordinator(
    private val onReload: () -> Unit,
    private val debounceMillis: Long = 1500,
    private var minReloadIntervalMillis: Long = 30000,
    private val maxAutoAttempts: Int = 3,
    private val scheduler: ReloadScheduler? = null,
) {

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val schedule: ReloadScheduler = scheduler ?: { task, delay ->
        handler.postDelayed(task, delay)
        val cancel: () -> Unit = { handler.removeCallbacks(task) }
        cancel
    }

    private var lastReloadTimeMillis = 0L
    private var pendingCancel: (() -> Unit)? = null
    private var autoAttempts = 0

    /**
     * Request a reload. [isAuto] marks automatic triggers that are subject to the attempt cap.
     * Triggers arriving within [debounceMillis] coalesce into a single reload.
     */
    fun scheduleReload(reason: String, isAuto: Boolean = false) {
        if (isAuto && autoAttempts >= maxAutoAttempts) return
        cancelPending()
        val cancel = schedule({ executeReload(isAuto) }, debounceMillis)
        pendingCancel = cancel
    }

    fun cancelPending() {
        pendingCancel?.invoke()
        pendingCancel = null
    }

    fun resetAutoAttempts() {
        autoAttempts = 0
    }

    fun configureMinInterval(millis: Long) {
        minReloadIntervalMillis = millis.coerceAtLeast(1000)
    }

    fun isPending(): Boolean = pendingCancel != null

    private fun executeReload(isAuto: Boolean) {
        pendingCancel = null
        val now = System.currentTimeMillis()
        val elapsed = now - lastReloadTimeMillis
        if (elapsed < minReloadIntervalMillis) {
            // Enforce a minimum gap between reloads to prevent reload loops.
            val cancel = schedule({ executeReload(isAuto) }, minReloadIntervalMillis - elapsed)
            pendingCancel = cancel
            return
        }
        lastReloadTimeMillis = now
        if (isAuto) autoAttempts++
        onReload()
    }
}

/** A fake, time-driven scheduler for deterministic unit tests. */
class FakeReloadScheduler {
    private class Entry(val task: () -> Unit, var delayMillis: Long) {
        var cancelled = false
        fun cancel() { cancelled = true }
    }
    private val entries = mutableListOf<Entry>()

    fun schedule(task: () -> Unit, delayMillis: Long): () -> Unit {
        val entry = Entry(task, delayMillis)
        entries.add(entry)
        return { entry.cancelled = true }
    }

    /** Returns the tasks that became due and "advances" the fake clock by [elapsedMillis]. */
    fun advance(elapsedMillis: Long): List<() -> Unit> {
        val due = entries.filter { !it.cancelled && it.delayMillis <= elapsedMillis }
        due.forEach { it.task() }
        entries.removeAll { it.cancelled || it in due }
        entries.forEach { it.delayMillis -= elapsedMillis }
        return due.map { it.task }
    }

    val pendingCount: Int get() = entries.count { !it.cancelled }
}
