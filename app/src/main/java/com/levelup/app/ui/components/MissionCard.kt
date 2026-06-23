package com.levelup.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.levelup.app.data.Difficulty
import com.levelup.app.data.MissionEntity
import com.levelup.app.data.MissionType
import com.levelup.app.data.StatType
import com.levelup.app.domain.DailyReset
import com.levelup.app.ui.theme.AccentCyan
import com.levelup.app.ui.theme.AccentGold
import com.levelup.app.ui.theme.AccentViolet
import com.levelup.app.ui.theme.HpRed

@Composable
fun MissionCard(
    mission: MissionEntity,
    onComplete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val type = runCatching { MissionType.valueOf(mission.type) }.getOrDefault(MissionType.SIDE)
    val stat = runCatching { StatType.valueOf(mission.stat) }.getOrDefault(StatType.DISCIPLINE)
    val diff = runCatching { Difficulty.valueOf(mission.difficulty) }.getOrDefault(Difficulty.NORMAL)
    val today = DailyReset.today()
    val isDone = when (type) {
        MissionType.DAILY -> mission.lastCompletedDate == today
        else -> mission.completed
    }
    val accent = when (type) {
        MissionType.MAIN -> AccentGold
        MissionType.SIDE -> AccentViolet
        MissionType.DAILY -> AccentCyan
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type stripe + complete tap target
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.width(44.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onComplete, enabled = !isDone) {
                        Icon(
                            imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = "Complete",
                            tint = if (isDone) accent else accent.copy(alpha = 0.85f)
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(text = type.name, color = accent)
                    Spacer(Modifier.width(6.dp))
                    Pill(text = stat.emoji, color = statColor(stat))
                    Spacer(Modifier.width(6.dp))
                    Pill(text = diff.displayName, color = difficultyColor(diff))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = mission.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                if (mission.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = mission.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = "XP",
                        tint = AccentGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${mission.xpReward} XP",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentGold
                    )
                    if (type == MissionType.DAILY && mission.currentStreak > 0) {
                        Spacer(Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = HpRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${mission.currentStreak}d",
                            style = MaterialTheme.typography.labelMedium,
                            color = HpRed
                        )
                    }
                }
            }
        }
    }
}

internal fun statColor(stat: StatType): Color = when (stat) {
    StatType.STRENGTH -> Color(0xFFFF7DB5)
    StatType.INTELLIGENCE -> AccentCyan
    StatType.DISCIPLINE -> AccentViolet
    StatType.VITALITY -> Color(0xFF69D6B5)
    StatType.SOCIAL -> AccentGold
}

internal fun difficultyColor(d: Difficulty): Color = when (d) {
    Difficulty.TRIVIAL -> Color(0xFF7F8CAA)
    Difficulty.EASY -> Color(0xFF69D6B5)
    Difficulty.NORMAL -> AccentCyan
    Difficulty.HARD -> AccentViolet
    Difficulty.EPIC -> Color(0xFFFF7DB5)
    Difficulty.LEGENDARY -> AccentGold
}
