package com.simplekioskbrowser.settings

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** A minimal in-memory SharedPreferences double for deterministic repository tests. */
private class FakeSharedPreferences(initial: Map<String, Any?> = emptyMap()) : SharedPreferences {
    private val map = mutableMapOf<String, Any?>().apply { putAll(initial) }

    override fun getAll(): Map<String, Any?> = map.toMap()
    override fun getString(key: String, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String, defValue: Set<String>?): Set<String>? =
        map[key] as? Set<String> ?: defValue
    override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor()
    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {}
    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {}

    private inner class FakeEditor : SharedPreferences.Editor {
        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            map[key] = value; return this
        }
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
            map[key] = values; return this
        }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            map[key] = value; return this
        }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            map[key] = value; return this
        }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            map[key] = value; return this
        }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            map[key] = value; return this
        }
        override fun remove(key: String): SharedPreferences.Editor {
            map.remove(key); return this
        }
        override fun clear(): SharedPreferences.Editor {
            map.clear(); return this
        }
        override fun commit(): Boolean = true
        override fun apply() {}
    }
}

class SettingsRepositoryTest {

    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        repo = SettingsRepository(FakeSharedPreferences())
    }

    @Test
    fun defaultsWhenNothingSaved() {
        val s = repo.getSettings()
        assertNull(s.url)
        assertFalse(s.reloadOnConnectivity)
        assertFalse(s.reloadOnWebViewError)
        assertFalse(s.periodicReloadEnabled)
        assertEquals(30, s.periodicInterval)
        assertEquals("minutes", s.periodicIntervalUnit)
        assertEquals(20, s.batteryThreshold)
        assertEquals(BatteryAction.NONE, s.batteryAction)
        assertEquals(OrientationLock.AUTO, s.orientationLock)
        assertEquals(30, s.autoReloadIntervalSeconds)
        assertFalse(s.hideSettingsAfterTimeout)
    }

    @Test
    fun saveValidSettingsRoundTrips() {
        val settings = KioskSettings(
            url = "https://example.com",
            reloadOnConnectivity = true,
            reloadOnWebViewError = true,
            periodicReloadEnabled = true,
            periodicInterval = 15,
            periodicIntervalUnit = "hours",
            batteryThreshold = 15,
            batteryAction = BatteryAction.PAUSE
        )
        assertTrue(repo.saveSettings(settings))
        assertEquals(settings, repo.getSettings())
    }

    @Test
    fun saveInvalidUrlIsRejectedAndPreviousRetained() {
        repo.saveSettings(KioskSettings(url = "https://example.com"))
        val before = repo.getSettings()

        val rejected = repo.saveSettings(KioskSettings(url = "not-a-url"))
        assertFalse(rejected)
        // Previous valid configuration is retained.
        assertEquals(before, repo.getSettings())
        assertEquals("https://example.com", repo.getSettings().url)
    }

    @Test
    fun isValidRejectsBadUrl() {
        assertFalse(repo.isValid(KioskSettings(url = "ftp://example.com")))
        assertFalse(repo.isValid(KioskSettings(url = "")))
        assertFalse(repo.isValid(KioskSettings(url = null)))
        assertTrue(repo.isValid(KioskSettings(url = "http://localhost:8080/")))
    }

    @Test
    fun saveUrlConvenience() {
        assertTrue(repo.saveUrl("https://kiosk.example"))
        assertEquals("https://kiosk.example", repo.getSettings().url)
        assertFalse(repo.saveUrl("garbage"))
        assertEquals("https://kiosk.example", repo.getSettings().url) // unchanged because invalid
    }

    @Test
    fun clearUrlRemovesUrl() {
        repo.saveSettings(KioskSettings(url = "https://example.com"))
        repo.clearUrl()
        assertNull(repo.getSettings().url)
    }

    @Test
    fun outOfRangeValuesAreInvalid() {
        assertFalse(repo.isValid(KioskSettings(url = "http://x", periodicInterval = -5)))
        assertFalse(repo.isValid(KioskSettings(url = "http://x", batteryThreshold = 999)))
        assertFalse(repo.isValid(KioskSettings(url = "http://x", periodicIntervalUnit = "days")))
    }
}
