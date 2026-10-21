package com.example.clockapp

import com.example.clockapp.ui.TimerInputLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerInputLogicTest {
    @Test
    fun digitsFillFromRightmostUnit() {
        // "5" = 5 seconds, "50" = 50 seconds, "500" = 5 minutes 0 seconds.
        assertEquals(5, TimerInputLogic.totalSeconds(TimerInputLogic.append("", 5)))
        assertEquals(50, TimerInputLogic.totalSeconds(TimerInputLogic.append("5", 0)))
        assertEquals(300, TimerInputLogic.totalSeconds(TimerInputLogic.append("50", 0)))
        assertEquals(3600, TimerInputLogic.totalSeconds("10000"))
    }

    @Test
    fun invalidPairIsRejected() {
        // 60 seconds cannot be typed: "6" ok, "60" rejected.
        assertEquals("6", TimerInputLogic.append("", 6))
        assertEquals("6", TimerInputLogic.append("6", 0))
        // Hours allow 99, minutes cap at 59: "999959" valid, "999960" not.
        assertTrue(TimerInputLogic.isValid("995959"))
        assertFalse(TimerInputLogic.isValid("996000"))
    }

    @Test
    fun maxSixDigits() {
        assertEquals("123456", TimerInputLogic.append("12345", 6))
        // Seventh digit ignored; "12:34:56" already at 6 digits.
        assertEquals("123456", TimerInputLogic.append("123456", 7))
    }


    @Test
    fun backspaceAndClear() {
        assertEquals("50", TimerInputLogic.backspace("500"))
        assertEquals("", TimerInputLogic.backspace("5"))
        assertEquals("", TimerInputLogic.backspace(""))
        assertEquals("", TimerInputLogic.clear())
    }

    @Test
    fun format_padsEveryUnit() {
        assertEquals("00:00:00", TimerInputLogic.format(""))
        assertEquals("00:00:05", TimerInputLogic.format("5"))
        assertEquals("00:05:00", TimerInputLogic.format("500"))
        assertEquals("01:00:00", TimerInputLogic.format("10000"))
        assertEquals("99:59:59", TimerInputLogic.format("995959"))
    }

    @Test
    fun digitsFor_presetRoundTrips() {
        for (seconds in listOf(60, 180, 300, 600, 900, 1800, 3600, 90, 1)) {
            val digits = TimerInputLogic.digitsFor(seconds)
            assertEquals(seconds, TimerInputLogic.totalSeconds(digits))
        }
    }
}
