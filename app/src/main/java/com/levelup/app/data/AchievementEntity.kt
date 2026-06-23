package com.levelup.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Earned achievement (e.g., "First Blood", "Rank C reached", "7-day streak").
 * The [code] is a stable identifier so we don't award the same one twice.
 */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val code: String,
    val title: String,
    val description: String,
    val unlockedAt: Long = System.currentTimeMillis()
)
