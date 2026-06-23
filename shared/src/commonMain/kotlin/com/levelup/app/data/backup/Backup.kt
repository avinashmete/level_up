package com.levelup.app.data.backup

import com.levelup.app.data.Achievement
import com.levelup.app.data.BackupSnapshot
import com.levelup.app.data.Completion
import com.levelup.app.data.Difficulty
import com.levelup.app.data.Mission
import com.levelup.app.data.MissionType
import com.levelup.app.data.Profile
import com.levelup.app.data.StatType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSON shape we persist for backups. Versioned so future imports can migrate.
 * Models are mirrored as plain serializable structs to keep schema migrations
 * decoupled from in-memory models.
 */
object Backup {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    const val VERSION = 1

    fun encode(snapshot: BackupSnapshot): String {
        val payload = BackupFile(
            version = VERSION,
            profile = snapshot.profile.toDto(),
            missions = snapshot.missions.map { it.toDto() },
            completions = snapshot.completions.map { it.toDto() },
            achievements = snapshot.achievements.map { it.toDto() }
        )
        return json.encodeToString(BackupFile.serializer(), payload)
    }

    fun decode(raw: String): BackupSnapshot {
        val payload = json.decodeFromString(BackupFile.serializer(), raw)
        return BackupSnapshot(
            profile = payload.profile.toModel(),
            missions = payload.missions.map { it.toModel() },
            completions = payload.completions.map { it.toModel() },
            achievements = payload.achievements.map { it.toModel() }
        )
    }
}

@Serializable
data class BackupFile(
    val version: Int,
    val profile: ProfileDto,
    val missions: List<MissionDto>,
    val completions: List<CompletionDto>,
    val achievements: List<AchievementDto>
)

@Serializable
data class ProfileDto(
    val id: Long = 1,
    val hunterName: String = "Hunter",
    val totalXp: Long = 0,
    val strength: Int = 0,
    val intelligence: Int = 0,
    val discipline: Int = 0,
    val vitality: Int = 0,
    val social: Int = 0,
    val lifetimeMissionsCompleted: Int = 0,
    val createdAt: Long
)

@Serializable
data class MissionDto(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val type: String,
    val stat: String,
    val difficulty: String,
    val xpReward: Int,
    val statReward: Int,
    val parentId: Long? = null,
    val createdAt: Long,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val archived: Boolean = false,
    val lastCompletedDate: String? = null,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
)

@Serializable
data class CompletionDto(
    val id: Long = 0,
    val missionId: Long,
    val missionTitle: String,
    val stat: String,
    val xpAwarded: Int,
    val statAwarded: Int,
    val completedAt: Long
)

@Serializable
data class AchievementDto(
    val code: String,
    val title: String,
    val description: String,
    val unlockedAt: Long
)

// --- Mapping helpers --------------------------------------------------------

private fun Profile.toDto() = ProfileDto(
    id = id, hunterName = hunterName, totalXp = totalXp,
    strength = strength, intelligence = intelligence, discipline = discipline,
    vitality = vitality, social = social,
    lifetimeMissionsCompleted = lifetimeMissionsCompleted, createdAt = createdAt
)

private fun ProfileDto.toModel() = Profile(
    id = id, hunterName = hunterName, totalXp = totalXp,
    strength = strength, intelligence = intelligence, discipline = discipline,
    vitality = vitality, social = social,
    lifetimeMissionsCompleted = lifetimeMissionsCompleted, createdAt = createdAt
)

private fun Mission.toDto() = MissionDto(
    id = id, title = title, description = description, type = type.name, stat = stat.name,
    difficulty = difficulty.name, xpReward = xpReward, statReward = statReward,
    parentId = parentId, createdAt = createdAt, completed = completed,
    completedAt = completedAt, archived = archived,
    lastCompletedDate = lastCompletedDate, currentStreak = currentStreak, bestStreak = bestStreak
)

private fun MissionDto.toModel() = Mission(
    id = id, title = title, description = description,
    type = runCatching { MissionType.valueOf(type) }.getOrDefault(MissionType.SIDE),
    stat = runCatching { StatType.valueOf(stat) }.getOrDefault(StatType.DISCIPLINE),
    difficulty = runCatching { Difficulty.valueOf(difficulty) }.getOrDefault(Difficulty.NORMAL),
    xpReward = xpReward, statReward = statReward,
    parentId = parentId, createdAt = createdAt, completed = completed,
    completedAt = completedAt, archived = archived,
    lastCompletedDate = lastCompletedDate, currentStreak = currentStreak, bestStreak = bestStreak
)

private fun Completion.toDto() = CompletionDto(
    id = id, missionId = missionId, missionTitle = missionTitle, stat = stat,
    xpAwarded = xpAwarded, statAwarded = statAwarded, completedAt = completedAt
)

private fun CompletionDto.toModel() = Completion(
    id = id, missionId = missionId, missionTitle = missionTitle, stat = stat,
    xpAwarded = xpAwarded, statAwarded = statAwarded, completedAt = completedAt
)

private fun Achievement.toDto() = AchievementDto(
    code = code, title = title, description = description, unlockedAt = unlockedAt
)

private fun AchievementDto.toModel() = Achievement(
    code = code, title = title, description = description, unlockedAt = unlockedAt
)
