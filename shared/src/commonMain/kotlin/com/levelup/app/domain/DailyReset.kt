package com.levelup.app.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Date helpers for daily-mission streak tracking. Uses kotlinx-datetime so the same
 * code runs on both JVM (Android) and Kotlin/Native (iOS).
 */
object DailyReset {

    fun today(timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
        Clock.System.now().toLocalDateTime(timeZone).date.toString()

    fun isYesterday(lastCompletedDate: String, today: String): Boolean {
        val last = runCatching { LocalDate.parse(lastCompletedDate) }.getOrNull() ?: return false
        val now = runCatching { LocalDate.parse(today) }.getOrNull() ?: return false
        return last.plus(DatePeriod(days = 1)) == now
    }

    fun nextStreak(lastCompletedDate: String?, today: String, currentStreak: Int): Int {
        return when {
            lastCompletedDate == null -> 1
            lastCompletedDate == today -> currentStreak // already counted
            isYesterday(lastCompletedDate, today) -> currentStreak + 1
            else -> 1 // gap broke the chain, restart at 1
        }
    }

    fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
