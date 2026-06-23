package com.levelup.app.domain

import kotlin.math.floor
import kotlin.math.pow

/**
 * Core progression math. Pure functions, no platform deps, easy to test on
 * both Android and iOS unit-test source sets.
 *
 * Level curve: each level requires `100 * level^1.55` XP. Buckets are designed so the
 * first promotion (Rank D) is reachable in a couple of weeks of consistent play.
 */
object Progression {

    const val MAX_LEVEL = 99

    /** Total XP required to *be* at the start of [level]. Level 1 is always 0. */
    fun xpRequiredFor(level: Int): Long {
        if (level <= 1) return 0
        var total = 0.0
        for (l in 1 until level) {
            total += 100.0 * l.toDouble().pow(1.55)
        }
        return floor(total).toLong()
    }

    fun levelFor(totalXp: Long): Int {
        if (totalXp <= 0) return 1
        var level = 1
        while (level < MAX_LEVEL && xpRequiredFor(level + 1) <= totalXp) {
            level++
        }
        return level
    }

    /**
     * XP progress within the current level: `(into, span)` where
     * `into` = XP earned past the start of the level, and
     * `span` = XP needed to reach the next level.
     */
    fun levelProgress(totalXp: Long): LevelProgress {
        val level = levelFor(totalXp)
        val into = totalXp - xpRequiredFor(level)
        val span = (xpRequiredFor(level + 1) - xpRequiredFor(level)).coerceAtLeast(1)
        return LevelProgress(level = level, xpIntoLevel = into, xpForNextLevel = span)
    }

    fun rankFor(level: Int): Rank = when {
        level >= 60 -> Rank.S
        level >= 45 -> Rank.A
        level >= 30 -> Rank.B
        level >= 18 -> Rank.C
        level >= 8 -> Rank.D
        else -> Rank.E
    }
}

data class LevelProgress(
    val level: Int,
    val xpIntoLevel: Long,
    val xpForNextLevel: Long
) {
    val fraction: Float = (xpIntoLevel.toFloat() / xpForNextLevel.toFloat()).coerceIn(0f, 1f)
}

/**
 * Hunter rank. Ordered E (lowest) -> S (highest) so we can compare with `>=` / `<`.
 */
enum class Rank(val label: String, val tagline: String) {
    E("E", "Awakened"),
    D("D", "Apprentice"),
    C("C", "Skilled"),
    B("B", "Veteran"),
    A("A", "Elite"),
    S("S", "Sovereign")
}
