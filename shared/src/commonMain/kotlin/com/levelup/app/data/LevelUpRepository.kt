package com.levelup.app.data

import com.levelup.app.data.db.LevelUpDatabase
import com.levelup.app.domain.DailyReset
import com.levelup.app.domain.Progression
import com.levelup.app.domain.Rank
import com.levelup.app.domain.Rewards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.levelup.app.data.db.Achievement as DbAchievement
import com.levelup.app.data.db.Completion as DbCompletion
import com.levelup.app.data.db.Mission as DbMission
import com.levelup.app.data.db.Profile as DbProfile

/**
 * Single facade for all read/write operations the UI layer needs.
 * Hides SQLDelight details, owns the "complete a mission" transaction
 * (XP + stats + streak + achievements).
 */
class LevelUpRepository(private val db: LevelUpDatabase) {

    // --- Flows -------------------------------------------------------------

    val profile: Flow<Profile?> = db.levelUpDatabaseQueries
        .selectProfile()
        .asFlow()
        .mapToOneOrNull(Dispatchers.Default)
        .map { it?.toModel() }

    val missions: Flow<List<Mission>> = db.levelUpDatabaseQueries
        .selectAllMissions()
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> rows.map { it.toModel() } }

    val achievements: Flow<List<Achievement>> = db.levelUpDatabaseQueries
        .selectAllAchievements()
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { rows -> rows.map { it.toModel() } }

    fun observeByType(type: MissionType): Flow<List<Mission>> =
        db.levelUpDatabaseQueries
            .selectMissionsByType(type.name)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel() } }

    fun dashboardFeed(): Flow<DashboardFeed> = combine(
        profile,
        observeByType(MissionType.DAILY),
        observeByType(MissionType.MAIN),
        observeByType(MissionType.SIDE)
    ) { p, daily, main, side -> DashboardFeed(p, daily, main, side) }

    // --- CRUD --------------------------------------------------------------

    suspend fun ensureProfile(): Profile {
        val existing = db.levelUpDatabaseQueries.selectProfile().executeAsOneOrNull()?.toModel()
        if (existing != null) return existing
        val now = DailyReset.nowMillis()
        val fresh = Profile(createdAt = now)
        upsertProfile(fresh)
        return fresh
    }

    suspend fun renameHunter(name: String) {
        val current = ensureProfile()
        upsertProfile(current.copy(hunterName = name.ifBlank { "Hunter" }))
    }

    suspend fun saveMission(mission: Mission): Long {
        val q = db.levelUpDatabaseQueries
        return if (mission.id == 0L) {
            q.insertMission(
                title = mission.title,
                description = mission.description,
                type = mission.type.name,
                stat = mission.stat.name,
                difficulty = mission.difficulty.name,
                xpReward = mission.xpReward.toLong(),
                statReward = mission.statReward.toLong(),
                parentId = mission.parentId,
                createdAt = mission.createdAt,
                completed = if (mission.completed) 1 else 0,
                completedAt = mission.completedAt,
                archived = if (mission.archived) 1 else 0,
                lastCompletedDate = mission.lastCompletedDate,
                currentStreak = mission.currentStreak.toLong(),
                bestStreak = mission.bestStreak.toLong()
            )
            q.lastInsertedId().executeAsOne()
        } else {
            q.updateMission(
                title = mission.title,
                description = mission.description,
                type = mission.type.name,
                stat = mission.stat.name,
                difficulty = mission.difficulty.name,
                xpReward = mission.xpReward.toLong(),
                statReward = mission.statReward.toLong(),
                parentId = mission.parentId,
                completed = if (mission.completed) 1 else 0,
                completedAt = mission.completedAt,
                archived = if (mission.archived) 1 else 0,
                lastCompletedDate = mission.lastCompletedDate,
                currentStreak = mission.currentStreak.toLong(),
                bestStreak = mission.bestStreak.toLong(),
                id = mission.id
            )
            mission.id
        }
    }

    suspend fun archive(id: Long) = db.levelUpDatabaseQueries.archiveMission(id)

    suspend fun deleteMission(id: Long) = db.levelUpDatabaseQueries.deleteMission(id)

    // --- Completion transaction -------------------------------------------

    suspend fun completeMission(
        missionId: Long,
        today: String = DailyReset.today(),
        nowMillis: Long = DailyReset.nowMillis()
    ): CompletionResult? {
        val q = db.levelUpDatabaseQueries
        val mission = q.selectMissionById(missionId).executeAsOneOrNull()?.toModel() ?: return null
        val profile = ensureProfile()

        when (mission.type) {
            MissionType.MAIN, MissionType.SIDE -> if (mission.completed) return null
            MissionType.DAILY -> if (mission.lastCompletedDate == today) return null
        }

        val xpGained = mission.xpReward
        val statGained = mission.statReward

        val updatedMission = when (mission.type) {
            MissionType.DAILY -> {
                val newStreak = DailyReset.nextStreak(mission.lastCompletedDate, today, mission.currentStreak)
                mission.copy(
                    lastCompletedDate = today,
                    currentStreak = newStreak,
                    bestStreak = maxOf(mission.bestStreak, newStreak)
                )
            }
            else -> mission.copy(completed = true, completedAt = nowMillis)
        }
        saveMission(updatedMission)

        q.insertCompletion(
            missionId = mission.id,
            missionTitle = mission.title,
            stat = mission.stat.name,
            xpAwarded = xpGained.toLong(),
            statAwarded = statGained.toLong(),
            completedAt = nowMillis
        )

        val beforeLevel = Progression.levelFor(profile.totalXp)
        val beforeRank = Progression.rankFor(beforeLevel)
        val newTotalXp = profile.totalXp + xpGained
        val afterLevel = Progression.levelFor(newTotalXp)
        val afterRank = Progression.rankFor(afterLevel)

        val updatedProfile = profile.copy(
            totalXp = newTotalXp,
            strength = profile.strength + if (mission.stat == StatType.STRENGTH) statGained else 0,
            intelligence = profile.intelligence + if (mission.stat == StatType.INTELLIGENCE) statGained else 0,
            discipline = profile.discipline + if (mission.stat == StatType.DISCIPLINE) statGained else 0,
            vitality = profile.vitality + if (mission.stat == StatType.VITALITY) statGained else 0,
            social = profile.social + if (mission.stat == StatType.SOCIAL) statGained else 0,
            lifetimeMissionsCompleted = profile.lifetimeMissionsCompleted + 1
        )
        upsertProfile(updatedProfile)

        val unlocked = Rewards.evaluate(
            profile = updatedProfile,
            justCompletedMission = updatedMission,
            previouslyUnlocked = q.selectAllAchievementsRaw().executeAsList().map { it.code }.toSet(),
            nowMillis = nowMillis
        )
        unlocked.forEach { a ->
            q.insertAchievementIfNew(a.code, a.title, a.description, a.unlockedAt)
        }

        return CompletionResult(
            missionTitle = updatedMission.title,
            xpGained = xpGained,
            statGained = statGained,
            statKey = mission.stat,
            leveledUp = afterLevel > beforeLevel,
            newLevel = afterLevel,
            rankedUp = afterRank != beforeRank,
            newRank = afterRank,
            currentStreak = updatedMission.currentStreak,
            newAchievements = unlocked
        )
    }

    suspend fun runDailyResetIfNeeded(today: String = DailyReset.today()) {
        val q = db.levelUpDatabaseQueries
        val list = q.selectAllMissionsRaw().executeAsList().map { it.toModel() }
            .filter { !it.archived && it.type == MissionType.DAILY }
        list.forEach { mission ->
            if (mission.lastCompletedDate == null) return@forEach
            if (mission.lastCompletedDate == today) return@forEach
            if (DailyReset.isYesterday(mission.lastCompletedDate, today)) return@forEach
            if (mission.currentStreak != 0) {
                saveMission(mission.copy(currentStreak = 0))
            }
        }
    }

    // --- Backup / restore --------------------------------------------------

    suspend fun snapshot(): BackupSnapshot {
        val q = db.levelUpDatabaseQueries
        return BackupSnapshot(
            profile = ensureProfile(),
            missions = q.selectAllMissionsRaw().executeAsList().map { it.toModel() },
            completions = q.selectAllCompletions().executeAsList().map { it.toModel() },
            achievements = q.selectAllAchievementsRaw().executeAsList().map { it.toModel() }
        )
    }

    suspend fun restore(snapshot: BackupSnapshot) {
        val q = db.levelUpDatabaseQueries
        q.clearMissions()
        q.clearCompletions()
        q.clearAchievements()
        q.clearProfile()

        upsertProfile(snapshot.profile.copy(id = 1))
        snapshot.missions.forEach { saveMission(it.copy(id = 0)) }
        snapshot.completions.forEach { c ->
            q.insertCompletion(
                missionId = c.missionId,
                missionTitle = c.missionTitle,
                stat = c.stat,
                xpAwarded = c.xpAwarded.toLong(),
                statAwarded = c.statAwarded.toLong(),
                completedAt = c.completedAt
            )
        }
        snapshot.achievements.forEach { a ->
            q.upsertAchievement(a.code, a.title, a.description, a.unlockedAt)
        }
    }

    // --- Internal helpers --------------------------------------------------

    private fun upsertProfile(p: Profile) {
        db.levelUpDatabaseQueries.upsertProfile(
            id = p.id,
            hunterName = p.hunterName,
            totalXp = p.totalXp,
            strength = p.strength.toLong(),
            intelligence = p.intelligence.toLong(),
            discipline = p.discipline.toLong(),
            vitality = p.vitality.toLong(),
            social = p.social.toLong(),
            lifetimeMissionsCompleted = p.lifetimeMissionsCompleted.toLong(),
            createdAt = p.createdAt
        )
    }
}

data class DashboardFeed(
    val profile: Profile?,
    val daily: List<Mission>,
    val main: List<Mission>,
    val side: List<Mission>
)

data class CompletionResult(
    val missionTitle: String,
    val xpGained: Int,
    val statGained: Int,
    val statKey: StatType,
    val leveledUp: Boolean,
    val newLevel: Int,
    val rankedUp: Boolean,
    val newRank: Rank,
    val currentStreak: Int,
    val newAchievements: List<Achievement>
)

data class BackupSnapshot(
    val profile: Profile,
    val missions: List<Mission>,
    val completions: List<Completion>,
    val achievements: List<Achievement>
)

// --- DB row -> model mappers -----------------------------------------------

private fun DbMission.toModel(): Mission = Mission(
    id = id,
    title = title,
    description = description,
    type = runCatching { MissionType.valueOf(type) }.getOrDefault(MissionType.SIDE),
    stat = runCatching { StatType.valueOf(stat) }.getOrDefault(StatType.DISCIPLINE),
    difficulty = runCatching { Difficulty.valueOf(difficulty) }.getOrDefault(Difficulty.NORMAL),
    xpReward = xpReward.toInt(),
    statReward = statReward.toInt(),
    parentId = parentId,
    createdAt = createdAt,
    completed = completed != 0L,
    completedAt = completedAt,
    archived = archived != 0L,
    lastCompletedDate = lastCompletedDate,
    currentStreak = currentStreak.toInt(),
    bestStreak = bestStreak.toInt()
)

private fun DbProfile.toModel(): Profile = Profile(
    id = id,
    hunterName = hunterName,
    totalXp = totalXp,
    strength = strength.toInt(),
    intelligence = intelligence.toInt(),
    discipline = discipline.toInt(),
    vitality = vitality.toInt(),
    social = social.toInt(),
    lifetimeMissionsCompleted = lifetimeMissionsCompleted.toInt(),
    createdAt = createdAt
)

private fun DbCompletion.toModel(): Completion = Completion(
    id = id,
    missionId = missionId,
    missionTitle = missionTitle,
    stat = stat,
    xpAwarded = xpAwarded.toInt(),
    statAwarded = statAwarded.toInt(),
    completedAt = completedAt
)

private fun DbAchievement.toModel(): Achievement = Achievement(
    code = code,
    title = title,
    description = description,
    unlockedAt = unlockedAt
)
