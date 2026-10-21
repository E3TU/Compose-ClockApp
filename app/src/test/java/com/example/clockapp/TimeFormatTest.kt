package com.example.clockapp

import com.example.clockapp.ui.formatDuration
import com.example.clockapp.ui.formatStopwatch
import com.example.clockapp.ui.lapSplits
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test
    fun formatDuration_ceilSeconds() {
        assertEquals("00:00", formatDuration(0))
        assertEquals("00:01", formatDuration(999))
        assertEquals("01:00", formatDuration(59_999))
        assertEquals("00:00", formatDuration(-1_000))
    }

    @Test
    fun formatDuration_minutesAndSeconds() {
        assertEquals("01:00", formatDuration(60_000))
        assertEquals("10:00", formatDuration(599_999))
        assertEquals("09:59", formatDuration(599_000))
        assertEquals("59:59", formatDuration(3_599_000))
    }

    @Test
    fun formatDuration_hours() {
        assertEquals("1:00:00", formatDuration(3_600_000))
        assertEquals("1:02:03", formatDuration(3_722_500))
        assertEquals("99:59:59", formatDuration(359_999_000))
    }

    @Test
    fun formatStopwatch_centisecondsUnderHour() {
        assertEquals("00:00.00", formatStopwatch(0))
        assertEquals("00:00.05", formatStopwatch(50))
        assertEquals("00:59.99", formatStopwatch(59_990))
        assertEquals("01:00.00", formatStopwatch(60_000))
    }

    @Test
    fun formatStopwatch_hoursDropCentiseconds() {
        assertEquals("1:00:00", formatStopwatch(3_600_000))
        assertEquals("2:03:04", formatStopwatch(7_384_000))
        assertEquals("00:00.00", formatStopwatch(-1_000))
    }

    @Test
    fun lapSplits_deltasFromTotals() {
        assertEquals(emptyList<Long>(), lapSplits(emptyList()))
        assertEquals(listOf(10_000L), lapSplits(listOf(10_000)))
        assertEquals(
            listOf(10_000L, 5_000L, 7_500L),
            lapSplits(listOf(10_000, 15_000, 22_500))
        )
    }
}
