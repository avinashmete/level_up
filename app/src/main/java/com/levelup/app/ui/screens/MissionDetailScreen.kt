package com.levelup.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.levelup.app.data.Difficulty
import com.levelup.app.data.MissionType
import com.levelup.app.data.StatType
import com.levelup.app.domain.DailyReset
import com.levelup.app.ui.LevelUpViewModel
import com.levelup.app.ui.components.Pill
import com.levelup.app.ui.components.SectionLabel
import com.levelup.app.ui.components.difficultyColor
import com.levelup.app.ui.components.statColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionDetailScreen(
    viewModel: LevelUpViewModel,
    missionId: Long,
    onBack: () -> Unit
) {
    val missionFlow = remember(missionId) {
        // Cheap re-derivation: filter the dashboard flow for the id.
        viewModel.dashboard
    }
    val feed by missionFlow.collectAsState()
    val mission = remember(feed, missionId) {
        (feed.daily + feed.main + feed.side).firstOrNull { it.id == missionId }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("QUEST DETAIL", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        if (mission == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Quest not found or archived.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        val type = runCatching { MissionType.valueOf(mission.type) }.getOrDefault(MissionType.SIDE)
        val stat = runCatching { StatType.valueOf(mission.stat) }.getOrDefault(StatType.DISCIPLINE)
        val diff = runCatching { Difficulty.valueOf(mission.difficulty) }.getOrDefault(Difficulty.NORMAL)
        val isDone = when (type) {
            MissionType.DAILY -> mission.lastCompletedDate == DailyReset.today()
            else -> mission.completed
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(PaddingValues(horizontal = 16.dp)),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row {
                        Pill(type.name, color = when (type) {
                            MissionType.MAIN -> MaterialTheme.colorScheme.tertiary
                            MissionType.SIDE -> MaterialTheme.colorScheme.secondary
                            MissionType.DAILY -> MaterialTheme.colorScheme.primary
                        })
                        Spacer(Modifier.width(6.dp))
                        Pill(stat.displayName, color = statColor(stat))
                        Spacer(Modifier.width(6.dp))
                        Pill(diff.displayName, color = difficultyColor(diff))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        mission.title,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (mission.description.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            mission.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SectionLabel("REWARDS")
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatColumn("XP", "+${mission.xpReward}")
                        StatColumn(stat.displayName, "+${mission.statReward}")
                        if (type == MissionType.DAILY) {
                            StatColumn("Streak", "${mission.currentStreak}d")
                            StatColumn("Best", "${mission.bestStreak}d")
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.complete(mission) },
                enabled = !isDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isDone) "ALREADY CLEARED" else "MARK COMPLETE")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.archive(mission.id); onBack() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Archive, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("ARCHIVE")
                }
                OutlinedButton(
                    onClick = { viewModel.delete(mission.id); onBack() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("DELETE")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Black)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
