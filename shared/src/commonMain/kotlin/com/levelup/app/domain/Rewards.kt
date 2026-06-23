package com.levelup.app.domain

import com.levelup.app.data.Achievement
import com.levelup.app.data.Mission
import com.levelup.app.data.MissionType
import com.levelup.app.data.Profile

/**
 * Pure-function achievement evaluator.
 * Given the post-completion state, returns any newly-unlocked achievements.
 * The caller is responsible for deduping against [previouslyUnlocked].
 */
object Rewards {

    fun evaluate(
        profile: Profile,
        justCompletedMission: Mission,
        previouslyUnlocked: Set<String>,
        nowMillis: Long
    ): List<Achievement> {
        val out = mutableListOf<Achievement>()
        val level = Progression.levelFor(profile.totalXp)
        val rank = Progression.rankFor(level)

        fun maybeAdd(code: String, title: String, description: String) {
            if (code !in previouslyUnlocked && out.none { it.code == code }) {
                out += Achievement(code, title, description, nowMillis)
            }
        }

        if (profile.lifetimeMissionsCompleted >= 1) {
            maybeAdd("first_blood", "First Blood", "Complete your first mission.")
        }

        val milestones = listOf(10, 25, 50, 100, 250, 500, 1000)
        milestones.firstOrNull { profile.lifetimeMissionsCompleted == it }?.let { n ->
            maybeAdd("missions_$n", "$n Missions", "Complete $n missions in total.")
        }

        val levelMilestones = listOf(5, 10, 20, 30, 50, 75, 99)
        levelMilestones.firstOrNull { level >= it && "level_$it" !in previouslyUnlocked }?.let { n ->
            maybeAdd("level_$n", "Level $n", "Reach character level $n.")
        }

        if (rank.ordinal >= Rank.D.ordinal) maybeAdd("rank_d", "Rank D", "Promoted to Rank D.")
        if (rank.ordinal >= Rank.C.ordinal) maybeAdd("rank_c", "Rank C", "Promoted to Rank C.")
        if (rank.ordinal >= Rank.B.ordinal) maybeAdd("rank_b", "Rank B", "Promoted to Rank B.")
        if (rank.ordinal >= Rank.A.ordinal) maybeAdd("rank_a", "Rank A", "Promoted to Rank A.")
        if (rank.ordinal >= Rank.S.ordinal) maybeAdd("rank_s", "Rank S", "Promoted to Rank S.")

        if (justCompletedMission.type == MissionType.DAILY) {
            val streak = justCompletedMission.currentStreak
            listOf(3, 7, 14, 30, 60, 100, 365).firstOrNull { it == streak }?.let { n ->
                maybeAdd("streak_$n", "$n-day Streak", "Complete a daily mission $n days in a row.")
            }
        }

        return out
    }
}
