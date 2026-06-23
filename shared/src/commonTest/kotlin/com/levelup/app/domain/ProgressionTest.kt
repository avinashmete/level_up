package com.levelup.app.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ProgressionTest {

    @Test
    fun level_1_starts_at_zero_xp() {
        assertEquals(0L, Progression.xpRequiredFor(1))
        assertEquals(1, Progression.levelFor(0))
        assertEquals(1, Progression.levelFor(-100))
    }

    @Test
    fun levels_are_monotonic_in_xp() {
        var prev = -1L
        for (level in 1..50) {
            val xp = Progression.xpRequiredFor(level)
            assertTrue(xp > prev, "level $level xp=$xp prev=$prev")
            prev = xp
        }
    }

    @Test
    fun levelFor_matches_xpRequiredFor() {
        for (level in 1..20) {
            val xpAtLevel = Progression.xpRequiredFor(level)
            assertEquals(level, Progression.levelFor(xpAtLevel))
            if (level > 1) {
                assertEquals(level - 1, Progression.levelFor(xpAtLevel - 1))
            }
        }
    }

    @Test
    fun rank_ladder_matches_buckets() {
        assertEquals(Rank.E, Progression.rankFor(1))
        assertEquals(Rank.E, Progression.rankFor(7))
        assertEquals(Rank.D, Progression.rankFor(8))
        assertEquals(Rank.D, Progression.rankFor(17))
        assertEquals(Rank.C, Progression.rankFor(18))
        assertEquals(Rank.B, Progression.rankFor(30))
        assertEquals(Rank.A, Progression.rankFor(45))
        assertEquals(Rank.S, Progression.rankFor(60))
        assertEquals(Rank.S, Progression.rankFor(99))
    }

    @Test
    fun progress_fraction_stays_in_zero_one() {
        val xp = Progression.xpRequiredFor(5) + 3
        val p = Progression.levelProgress(xp)
        assertEquals(5, p.level)
        assertTrue(p.fraction in 0f..1f)
        assertNotEquals(0f, p.fraction)
    }
}
