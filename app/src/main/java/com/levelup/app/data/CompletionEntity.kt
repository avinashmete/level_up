package com.levelup.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per "the user completed a mission" event.
 * Used to compute totals and to show the activity log on the profile screen.
 */
@Entity(tableName = "completions")
data class CompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val missionId: Long,
    val missionTitle: String, // denormalized so deleted missions still show in history
    val stat: String,
    val xpAwarded: Int,
    val statAwarded: Int,
    val completedAt: Long = System.currentTimeMillis()
)
