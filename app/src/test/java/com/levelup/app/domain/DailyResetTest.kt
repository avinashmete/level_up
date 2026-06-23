package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyResetTest {

    @Test
    fun `first completion sets streak to 1`() {
        assertEquals(1, DailyReset.nextStreak(lastCompletedDate = null, today = "2026-06-23", currentStreak = 0))
    }

    @Test
    fun `same-day repeat does not bump streak`() {
        assertEquals(3, DailyReset.nextStreak("2026-06-23", "2026-06-23", 3))
    }

    @Test
    fun `yesterday continues streak`() {
        assertEquals(4, DailyReset.nextStreak("2026-06-22", "2026-06-23", 3))
        assertTrue(DailyReset.isYesterday("2026-06-22", "2026-06-23"))
    }

    @Test
    fun `gap resets to 1`() {
        assertEquals(1, DailyReset.nextStreak("2026-06-20", "2026-06-23", 9))
        assertFalse(DailyReset.isYesterday("2026-06-20", "2026-06-23"))
    }

    @Test
    fun `garbage date is safe`() {
        assertFalse(DailyReset.isYesterday("not-a-date", "2026-06-23"))
    }
}
