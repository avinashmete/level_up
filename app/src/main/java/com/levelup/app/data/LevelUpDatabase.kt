package com.levelup.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MissionEntity::class,
        CompletionEntity::class,
        ProfileEntity::class,
        AchievementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LevelUpDatabase : RoomDatabase() {
    abstract fun missionDao(): MissionDao
    abstract fun completionDao(): CompletionDao
    abstract fun profileDao(): ProfileDao
    abstract fun achievementDao(): AchievementDao
}
