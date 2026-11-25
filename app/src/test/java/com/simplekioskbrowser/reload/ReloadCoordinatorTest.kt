package com.simplekioskbrowser.reload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReloadCoordinatorTest {

    private var reloads = 0

    private fun newCoordinator(
        fake: FakeReloadScheduler,
        debounce: Long = 0,
        minInterval: Long = 0,
        maxAuto: Int = 3,
    ): ReloadCoordinator {
        reloads = 0
        return ReloadCoordinator({ reloads++ }, debounce, minInterval, maxAuto, fake::schedule)
    }

    @Test
    fun multipleTriggersCoalesceIntoOneReload() {
        val fake = FakeReloadScheduler()
        val c = newCoordinator(fake, debounce = 100)
        c.scheduleReload("a")
        c.scheduleReload("b")
        c.scheduleReload("c")
        assertEquals(1, fake.pendingCount) // coalesced to a single pending task
        fake.advance(99)
        assertEquals(0, reloads) // debounce window not yet elapsed
        fake.advance(1)
        assertEquals(1, reloads) // debounce elapsed -> exactly one reload
        assertEquals(0, fake.pendingCount)
    }

    @Test
    fun cancelPendingRemovesScheduledReload() {
        val fake = FakeReloadScheduler()
        val c = newCoordinator(fake, debounce = 100)
        c.scheduleReload("a")
        assertEquals(1, fake.pendingCount)
        c.cancelPending()
        assertEquals(0, fake.pendingCount)
        fake.advance(500)
        assertEquals(0, reloads) // never fired
    }

    @Test
    fun autoReloadsAreCappedToPreventLoops() {
        val fake = FakeReloadScheduler()
        val c = newCoordinator(fake, debounce = 0, minInterval = 0, maxAuto = 3)
        repeat(4) { c.scheduleReload("x", isAuto = true); fake.advance(0) }
        assertEquals(3, reloads) // hard cap on automatic attempts
        assertFalse(c.isPending())

        c.resetAutoAttempts()
        c.scheduleReload("x", isAuto = true)
        fake.advance(0)
        assertEquals(4, reloads) // usable again after reset
    }

    @Test
    fun manualReloadsAreNotCapped() {
        val fake = FakeReloadScheduler()
        val c = newCoordinator(fake, debounce = 0, minInterval = 0, maxAuto = 3)
        repeat(6) { c.scheduleReload("m", isAuto = false); fake.advance(0) }
        assertEquals(6, reloads) // user-initiated reloads are never capped
    }

    @Test
    fun minIntervalDefersRapidReloads() {
        val fake = FakeReloadScheduler()
        val c = newCoordinator(fake, debounce = 0, minInterval = 60_000, maxAuto = 3)
        c.scheduleReload("a", isAuto = false)
        fake.advance(0)
        assertEquals(1, reloads)
        assertFalse(c.isPending())
        // Real-time minimum interval: an immediate second reload is deferred.
        c.scheduleReload("b", isAuto = false)
        assertTrue(c.isPending())
        assertEquals(1, reloads) // not fired yet
    }
}
