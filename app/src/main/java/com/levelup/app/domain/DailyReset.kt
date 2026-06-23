package com.levelup.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Date helpers for daily-mission streak tracking. Kept here so unit tests can swap
 * the "today" string instead of having to mock the system clock.
 */
object DailyReset {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd

    fun today(): String = LocalDate.now().format(formatter)

    fun isYesterday(lastCompletedDate: String, today: String): Boolean {
        return try {
            val last = LocalDate.parse(lastCompletedDate, formatter)
            val now = LocalDate.parse(today, formatter)
            last.plusDays(1) == now
        } catch (_: Throwable) {
            false
        }
    }

    fun nextStreak(lastCompletedDate: String?, today: String, currentStreak: Int): Int {
        return when {
            lastCompletedDate == null -> 1
            lastCompletedDate == today -> currentStreak // already counted
            isYesterday(lastCompletedDate, today) -> currentStreak + 1
            else -> 1 // gap broke the chain, restart at 1
        }
    }
}
