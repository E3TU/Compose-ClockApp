package com.example.clockapp

import com.example.clockapp.data.TimerScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimerSchedulerTest {
    @Test
    fun triggerAt_returnsEndsAt_whenInFuture() {
        assertEquals(2_000L, TimerScheduler.triggerAt(1_000L, 2_000L))
    }

    @Test
    fun triggerAt_null_whenAlreadyPassed() {
        assertNull(TimerScheduler.triggerAt(2_000L, 1_000L))
        assertNull(TimerScheduler.triggerAt(2_000L, 2_000L))
    }

    @Test
    fun triggerAt_null_whenNotScheduled() {
        assertNull(TimerScheduler.triggerAt(1_000L, 0L))
    }
}
