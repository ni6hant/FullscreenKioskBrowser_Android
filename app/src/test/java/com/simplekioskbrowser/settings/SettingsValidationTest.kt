package com.simplekioskbrowser.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure validation logic: no Android dependencies, fast and deterministic. */
class SettingsValidationTest {

    @Test
    fun acceptsValidHttpUrl() {
        assertTrue(SettingsValidation.isValidUrl("http://example.com"))
    }

    @Test
    fun acceptsValidHttpsUrl() {
        assertTrue(SettingsValidation.isValidUrl("https://example.com"))
    }

    @Test
    fun acceptsIntranetHostWithoutTld() {
        // A kiosk may point at a local server; a TLD is not required, only a valid scheme.
        assertTrue(SettingsValidation.isValidUrl("http://192.168.1.50/"))
        assertTrue(SettingsValidation.isValidUrl("https://my-local-server"))
    }

    @Test
    fun rejectsNonWebSchemes() {
        assertFalse(SettingsValidation.isValidUrl("ftp://example.com"))
        assertFalse(SettingsValidation.isValidUrl("file:///etc/passwd"))
        assertFalse(SettingsValidation.isValidUrl("mailto:test@example.com"))
        assertFalse(SettingsValidation.isValidUrl("about:blank"))
    }

    @Test
    fun rejectsMissingOrInvalidScheme() {
        assertFalse(SettingsValidation.isValidUrl("example.com"))
        assertFalse(SettingsValidation.isValidUrl("//example.com"))
        assertFalse(SettingsValidation.isValidUrl("ht tp://example.com"))
    }

    @Test
    fun rejectsEmptyAndBlank() {
        assertFalse(SettingsValidation.isValidUrl(""))
        assertFalse(SettingsValidation.isValidUrl("   "))
        assertFalse(SettingsValidation.isValidUrl(null))
    }

    @Test
    fun rejectsMalformed() {
        assertFalse(SettingsValidation.isValidUrl("http://"))
        assertFalse(SettingsValidation.isValidUrl("https://? "))
    }

    @Test
    fun intervalBounds() {
        assertTrue(SettingsValidation.isValidInterval(1))
        assertTrue(SettingsValidation.isValidInterval(30))
        assertTrue(SettingsValidation.isValidInterval(10_080)) // 7 days
        assertFalse(SettingsValidation.isValidInterval(0))
        assertFalse(SettingsValidation.isValidInterval(-5))
        assertFalse(SettingsValidation.isValidInterval(10_081))
    }

    @Test
    fun intervalUnits() {
        assertTrue(SettingsValidation.isValidIntervalUnit("minutes"))
        assertTrue(SettingsValidation.isValidIntervalUnit("hours"))
        assertFalse(SettingsValidation.isValidIntervalUnit("days"))
        assertFalse(SettingsValidation.isValidIntervalUnit(""))
    }

    @Test
    fun batteryThresholdBounds() {
        assertTrue(SettingsValidation.isValidBatteryThreshold(0))
        assertTrue(SettingsValidation.isValidBatteryThreshold(20)) // default
        assertTrue(SettingsValidation.isValidBatteryThreshold(100))
        assertFalse(SettingsValidation.isValidBatteryThreshold(-1))
        assertFalse(SettingsValidation.isValidBatteryThreshold(101))
    }

    @Test
    fun batteryActions() {
        assertTrue(SettingsValidation.isValidBatteryAction(BatteryAction.NONE))
        assertTrue(SettingsValidation.isValidBatteryAction(BatteryAction.DIM))
        assertTrue(SettingsValidation.isValidBatteryAction(BatteryAction.PAUSE))
        assertFalse(SettingsValidation.isValidBatteryAction("poweroff"))
        assertFalse(SettingsValidation.isValidBatteryAction(""))
    }

    @Test
    fun intervalToMinutes() {
        assertEquals(30, SettingsValidation.intervalToMinutes(30, "minutes"))
        assertEquals(90, SettingsValidation.intervalToMinutes(90, "minutes"))
        assertEquals(120, SettingsValidation.intervalToMinutes(2, "hours"))
        assertEquals(60, SettingsValidation.intervalToMinutes(1, "hours"))
    }
}
