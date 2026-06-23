package com.levelup.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Singleton player profile (id is always 1).
 * totalXp is the source of truth; level/rank are derived in [com.levelup.app.domain.Progression].
 * Stats are tracked per dimension and can grow without bound.
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Long = 1,
    val hunterName: String = "Hunter",
    val totalXp: Long = 0,
    val strength: Int = 0,
    val intelligence: Int = 0,
    val discipline: Int = 0,
    val vitality: Int = 0,
    val social: Int = 0,
    val lifetimeMissionsCompleted: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
