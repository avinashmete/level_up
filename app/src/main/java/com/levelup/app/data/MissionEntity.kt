package com.levelup.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single goal/quest the user has committed to.
 *
 * - [MissionType.MAIN] - long arc, completed once when the user marks it done.
 * - [MissionType.SIDE] - optional one-off quest, completed once.
 * - [MissionType.DAILY] - recurring habit that resets each day.
 */
@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val type: String, // MissionType name
    val stat: String, // StatType name
    val difficulty: String, // Difficulty name
    val xpReward: Int,
    val statReward: Int, // points added to [stat] on completion
    val parentId: Long? = null, // optional: side mission tied to a main mission
    val createdAt: Long = System.currentTimeMillis(),
    val completed: Boolean = false, // used for MAIN/SIDE; daily uses lastCompletedDate
    val completedAt: Long? = null,
    val archived: Boolean = false,
    // Daily-only state
    val lastCompletedDate: String? = null, // yyyy-MM-dd in local TZ
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
)

enum class MissionType { MAIN, SIDE, DAILY }

enum class StatType(val displayName: String, val emoji: String) {
    STRENGTH("Strength", "STR"),
    INTELLIGENCE("Intelligence", "INT"),
    DISCIPLINE("Discipline", "DIS"),
    VITALITY("Vitality", "VIT"),
    SOCIAL("Social", "SOC")
}

enum class Difficulty(val displayName: String, val xp: Int, val statPoints: Int) {
    TRIVIAL("Trivial", 10, 1),
    EASY("Easy", 25, 1),
    NORMAL("Normal", 60, 2),
    HARD("Hard", 140, 3),
    EPIC("Epic", 320, 5),
    LEGENDARY("Legendary", 700, 8)
}
