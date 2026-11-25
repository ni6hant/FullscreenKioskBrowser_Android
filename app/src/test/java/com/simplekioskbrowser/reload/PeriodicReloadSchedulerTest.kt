package com.simplekioskbrowser.reload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodicReloadSchedulerTest {

    private var reloads = 0

    private fun newScheduler(fake: FakeReloadScheduler): PeriodicReloadScheduler {
        reloads = 0
        return PeriodicReloadScheduler({ reloads++ }, fake::schedule)
    }

    @Test
    fun schedulesExactlyOneTaskWhenEnabled() {
        val fake = FakeReloadScheduler()
        val s = newScheduler(fake)
        s.configure(true, 1000)
        assertEquals(1, fake.pendingCount)
    }

    @Test
    fun reconfiguringDoesNotDuplicateTimers() {
        val fake = FakeReloadScheduler()
        val s = newScheduler(fake)
        s.configure(true, 1000)
        s.configure(true, 2000)
        assertEquals(1, fake.pendingCount) // old timer cancelled before new one created
    }

    @Test
    fun disabledHasNoPendingTask() {
        val fake = FakeReloadScheduler()
        val s = newScheduler(fake)
        s.configure(false, 1000)
        assertEquals(0, fake.pendingCount)
        assertFalse(s.isEnabled())
    }

    @Test
    fun intervalIsRespectedBeforeFiring() {
        val fake = FakeReloadScheduler()
        val s = newScheduler(fake)
        s.configure(true, 500)
        fake.advance(499)
        assertEquals(0, reloads)
        fake.advance(1)
        assertEquals(1, reloads)
    }

    @Test
    fun taskReschedulesItselfMaintainingLoop() {
        val fake = FakeReloadScheduler()
        val s = newScheduler(fake)
        s.configure(true, 250)
        fake.advance(250)
        assertEquals(1, reloads)
        assertEquals(1, fake.pendingCount) // rescheduled automatically
        fake.advance(250)
        assertEquals(2, reloads)
    }

    @Test
    fun pauseAndResumeAreLifecycleAware() {
        val fake = FakeReloadScheduler()
        val s = newScheduler(fake)
        s.configure(true, 1000)
        fake.advance(1000)
        assertEquals(1, reloads)
        s.pause()
        assertEquals(0, fake.pendingCount) // timer cancelled when not foregrounded
        fake.advance(1000)
        assertEquals(1, reloads) // no new reload while paused
        s.resume()
        assertEquals(1, fake.pendingCount) // timer restored when foregrounded
        fake.advance(1000)
        assertEquals(2, reloads)
    }
}
