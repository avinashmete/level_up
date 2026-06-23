package com.levelup.app.data

import com.levelup.app.domain.DailyReset
import com.levelup.app.domain.Progression
import com.levelup.app.domain.Rewards
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Single facade for all read/write operations the UI layer needs.
 *
 * Hides Room details, owns the "complete a mission" transaction (XP + stats + streak +
 * achievements), and exposes hot flows the ViewModels collect.
 */
class LevelUpRepository(
    private val missionDao: MissionDao,
    private val completionDao: CompletionDao,
    private val profileDao: ProfileDao,
    private val achievementDao: AchievementDao
) {

    // --- Flows -------------------------------------------------------------

    val profile: Flow<ProfileEntity?> = profileDao.observe()
    val missions: Flow<List<MissionEntity>> = missionDao.observeAll()
    val recentCompletions: Flow<List<CompletionEntity>> = completionDao.observeRecent(50)
    val achievements: Flow<List<AchievementEntity>> = achievementDao.observeAll()

    fun observeByType(type: MissionType): Flow<List<MissionEntity>> =
        missionDao.observeByType(type.name)

    fun observeMission(id: Long): Flow<MissionEntity?> = missionDao.observeById(id)

    /**
     * Today's daily missions plus the profile, useful for the dashboard.
     */
    fun dashboardFeed(): Flow<DashboardFeed> = combine(
        profileDao.observe(),
        missionDao.observeByType(MissionType.DAILY.name),
        missionDao.observeByType(MissionType.MAIN.name),
        missionDao.observeByType(MissionType.SIDE.name)
    ) { profile, daily, main, side ->
        DashboardFeed(profile, daily, main, side)
    }

    // --- CRUD --------------------------------------------------------------

    suspend fun ensureProfile(): ProfileEntity {
        val existing = profileDao.get()
        if (existing != null) return existing
        val fresh = ProfileEntity()
        profileDao.upsert(fresh)
        return fresh
    }

    suspend fun renameHunter(name: String) {
        val current = ensureProfile()
        profileDao.upsert(current.copy(hunterName = name.ifBlank { "Hunter" }))
    }

    suspend fun saveMission(mission: MissionEntity): Long {
        return if (mission.id == 0L) missionDao.insert(mission)
        else { missionDao.update(mission); mission.id }
    }

    suspend fun archive(id: Long) = missionDao.archive(id)

    suspend fun deleteMission(id: Long) = missionDao.delete(id)

    // --- Completion transaction -------------------------------------------

    /**
     * Awards XP/stat, logs the completion, updates the profile, applies streak logic for
     * daily missions, and unlocks any newly-earned achievements.
     *
     * Returns a [CompletionResult] the UI can use to render level-up screens, rank-ups, etc.
     */
    suspend fun completeMission(missionId: Long, today: String = DailyReset.today()): CompletionResult? {
        val mission = missionDao.findById(missionId) ?: return null
        val profile = ensureProfile()
        val type = runCatching { MissionType.valueOf(mission.type) }.getOrDefault(MissionType.SIDE)

        // Guard rails - already completed for today / forever
        when (type) {
            MissionType.MAIN, MissionType.SIDE -> if (mission.completed) return null
            MissionType.DAILY -> if (mission.lastCompletedDate == today) return null
        }

        val xpGained = mission.xpReward
        val statGained = mission.statReward
        val statKey = runCatching { StatType.valueOf(mission.stat) }.getOrDefault(StatType.DISCIPLINE)

        // Update mission row
        val updatedMission = when (type) {
            MissionType.DAILY -> {
                val newStreak = DailyReset.nextStreak(mission.lastCompletedDate, today, mission.currentStreak)
                mission.copy(
                    lastCompletedDate = today,
                    currentStreak = newStreak,
                    bestStreak = maxOf(mission.bestStreak, newStreak)
                )
            }
            else -> mission.copy(completed = true, completedAt = System.currentTimeMillis())
        }
        missionDao.update(updatedMission)

        // Log completion
        completionDao.insert(
            CompletionEntity(
                missionId = mission.id,
                missionTitle = mission.title,
                stat = mission.stat,
                xpAwarded = xpGained,
                statAwarded = statGained
            )
        )

        // Update profile
        val beforeLevel = Progression.levelFor(profile.totalXp)
        val beforeRank = Progression.rankFor(beforeLevel)
        val newTotalXp = profile.totalXp + xpGained
        val afterLevel = Progression.levelFor(newTotalXp)
        val afterRank = Progression.rankFor(afterLevel)

        val updatedProfile = profile.copy(
            totalXp = newTotalXp,
            strength = profile.strength + if (statKey == StatType.STRENGTH) statGained else 0,
            intelligence = profile.intelligence + if (statKey == StatType.INTELLIGENCE) statGained else 0,
            discipline = profile.discipline + if (statKey == StatType.DISCIPLINE) statGained else 0,
            vitality = profile.vitality + if (statKey == StatType.VITALITY) statGained else 0,
            social = profile.social + if (statKey == StatType.SOCIAL) statGained else 0,
            lifetimeMissionsCompleted = profile.lifetimeMissionsCompleted + 1
        )
        profileDao.upsert(updatedProfile)

        // Achievements
        val unlocked = Rewards.evaluate(
            profile = updatedProfile,
            justCompletedMission = updatedMission,
            previouslyUnlocked = achievementDao.all().map { it.code }.toSet()
        )
        unlocked.forEach { achievementDao.insertIfNew(it) }

        return CompletionResult(
            missionTitle = updatedMission.title,
            xpGained = xpGained,
            statGained = statGained,
            statKey = statKey,
            leveledUp = afterLevel > beforeLevel,
            newLevel = afterLevel,
            rankedUp = afterRank != beforeRank,
            newRank = afterRank,
            currentStreak = updatedMission.currentStreak,
            newAchievements = unlocked
        )
    }

    /**
     * Daily missions: clear streaks for any whose [lastCompletedDate] is older than
     * yesterday. Run once on app start.
     */
    suspend fun runDailyResetIfNeeded(today: String = DailyReset.today()) {
        val list = missionDao.all().filter {
            !it.archived && it.type == MissionType.DAILY.name
        }
        list.forEach { mission ->
            if (mission.lastCompletedDate == null) return@forEach
            if (mission.lastCompletedDate == today) return@forEach
            if (DailyReset.isYesterday(mission.lastCompletedDate, today)) return@forEach
            // Missed -> streak broken
            if (mission.currentStreak != 0) {
                missionDao.update(mission.copy(currentStreak = 0))
            }
        }
    }

    // --- Backup / restore --------------------------------------------------

    suspend fun snapshot(): BackupSnapshot = BackupSnapshot(
        profile = ensureProfile(),
        missions = missionDao.all(),
        completions = completionDao.all(),
        achievements = achievementDao.all()
    )

    suspend fun restore(snapshot: BackupSnapshot) {
        missionDao.clear()
        completionDao.clear()
        achievementDao.clear()
        profileDao.clear()

        profileDao.upsert(snapshot.profile.copy(id = 1))
        snapshot.missions.forEach { missionDao.insert(it.copy(id = 0)) }
        snapshot.completions.forEach { completionDao.insert(it.copy(id = 0)) }
        snapshot.achievements.forEach { achievementDao.upsert(it) }
    }
}

data class DashboardFeed(
    val profile: ProfileEntity?,
    val daily: List<MissionEntity>,
    val main: List<MissionEntity>,
    val side: List<MissionEntity>
)

data class CompletionResult(
    val missionTitle: String,
    val xpGained: Int,
    val statGained: Int,
    val statKey: StatType,
    val leveledUp: Boolean,
    val newLevel: Int,
    val rankedUp: Boolean,
    val newRank: com.levelup.app.domain.Rank,
    val currentStreak: Int,
    val newAchievements: List<AchievementEntity>
)

data class BackupSnapshot(
    val profile: ProfileEntity,
    val missions: List<MissionEntity>,
    val completions: List<CompletionEntity>,
    val achievements: List<AchievementEntity>
)
