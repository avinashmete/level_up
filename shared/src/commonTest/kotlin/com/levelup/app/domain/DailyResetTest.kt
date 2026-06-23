package com.levelup.app.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyResetTest {

    @Test
    fun first_completion_sets_streak_to_one() {
        assertEquals(1, DailyReset.nextStreak(lastCompletedDate = null, today = "2026-06-23", currentStreak = 0))
    }

    @Test
    fun same_day_repeat_does_not_bump_streak() {
        assertEquals(3, DailyReset.nextStreak("2026-06-23", "2026-06-23", 3))
    }

    @Test
    fun yesterday_continues_streak() {
        assertEquals(4, DailyReset.nextStreak("2026-06-22", "2026-06-23", 3))
        assertTrue(DailyReset.isYesterday("2026-06-22", "2026-06-23"))
    }

    @Test
    fun gap_resets_to_one() {
        assertEquals(1, DailyReset.nextStreak("2026-06-20", "2026-06-23", 9))
        assertFalse(DailyReset.isYesterday("2026-06-20", "2026-06-23"))
    }

    @Test
    fun garbage_date_is_safe() {
        assertFalse(DailyReset.isYesterday("not-a-date", "2026-06-23"))
    }
}
