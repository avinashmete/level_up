package com.levelup.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {

    @Test
    fun `level 1 starts at zero xp`() {
        assertEquals(0L, Progression.xpRequiredFor(1))
        assertEquals(1, Progression.levelFor(0))
        assertEquals(1, Progression.levelFor(-100))
    }

    @Test
    fun `levels are monotonic in xp`() {
        var prev = -1L
        for (level in 1..50) {
            val xp = Progression.xpRequiredFor(level)
            assertTrue("level $level xp=$xp prev=$prev", xp > prev)
            prev = xp
        }
    }

    @Test
    fun `levelFor matches xpRequiredFor`() {
        for (level in 1..20) {
            val xpAtLevel = Progression.xpRequiredFor(level)
            assertEquals(level, Progression.levelFor(xpAtLevel))
            // one less should be the previous level
            if (level > 1) {
                assertEquals(level - 1, Progression.levelFor(xpAtLevel - 1))
            }
        }
    }

    @Test
    fun `rank ladder matches the buckets`() {
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
    fun `progress fraction stays in 0_1`() {
        val xp = Progression.xpRequiredFor(5) + 3 // small progress into level 5
        val p = Progression.levelProgress(xp)
        assertEquals(5, p.level)
        assertTrue(p.fraction in 0f..1f)
        assertNotEquals(0f, p.fraction)
    }
}
