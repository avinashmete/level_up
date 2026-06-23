package com.levelup.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {

    @Query("SELECT * FROM missions WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MissionEntity>>

    @Query("SELECT * FROM missions WHERE archived = 0 AND type = :type ORDER BY createdAt DESC")
    fun observeByType(type: String): Flow<List<MissionEntity>>

    @Query("SELECT * FROM missions WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): MissionEntity?

    @Query("SELECT * FROM missions WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<MissionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mission: MissionEntity): Long

    @Update
    suspend fun update(mission: MissionEntity)

    @Query("UPDATE missions SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("DELETE FROM missions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM missions")
    suspend fun all(): List<MissionEntity>

    @Query("DELETE FROM missions")
    suspend fun clear()
}

@Dao
interface CompletionDao {

    @Query("SELECT * FROM completions ORDER BY completedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<CompletionEntity>>

    @Insert
    suspend fun insert(completion: CompletionEntity): Long

    @Query("SELECT * FROM completions")
    suspend fun all(): List<CompletionEntity>

    @Query("DELETE FROM completions")
    suspend fun clear()
}

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    suspend fun get(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)

    @Query("DELETE FROM profile")
    suspend fun clear()
}

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun all(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(achievement: AchievementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(achievement: AchievementEntity)

    @Query("DELETE FROM achievements")
    suspend fun clear()
}
