package com.simplekioskbrowser.battery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryThresholdTest {

    @Test
    fun belowThresholdNotPluggedIsLow() {
        assertTrue(isLowBattery(BatteryInfo(15, 100, 15, false), 20))
    }

    @Test
    fun atThresholdIsLow() {
        assertTrue(isLowBattery(BatteryInfo(20, 100, 20, false), 20)) // boundary inclusive
    }

    @Test
    fun aboveThresholdIsNotLow() {
        assertFalse(isLowBattery(BatteryInfo(21, 100, 21, false), 20))
    }

    @Test
    fun chargingPreventsLowBatteryActivation() {
        assertFalse(isLowBattery(BatteryInfo(5, 100, 5, true), 20)) // plugged in
    }

    @Test
    fun zeroThresholdNeverActivates() {
        assertFalse(isLowBattery(BatteryInfo(1, 100, 1, false), 0))
    }

    @Test
    fun hundredThresholdActivatesWhenNotPlugged() {
        assertTrue(isLowBattery(BatteryInfo(99, 100, 99, false), 100))
        assertFalse(isLowBattery(BatteryInfo(100, 100, 100, true), 100)) // plugged
    }

    @Test
    fun unknownLevelIsNotLow() {
        assertFalse(isLowBattery(BatteryInfo(-1, -1, -1, false), 20))
    }
}
