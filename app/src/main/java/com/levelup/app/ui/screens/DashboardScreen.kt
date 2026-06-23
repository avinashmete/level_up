package com.levelup.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.levelup.app.data.DashboardFeed
import com.levelup.app.data.MissionEntity
import com.levelup.app.data.MissionType
import com.levelup.app.domain.DailyReset
import com.levelup.app.domain.Progression
import com.levelup.app.domain.Rank
import com.levelup.app.ui.LevelUpViewModel
import com.levelup.app.ui.components.MissionCard
import com.levelup.app.ui.components.Pill
import com.levelup.app.ui.components.RankBadge
import com.levelup.app.ui.components.SectionLabel
import com.levelup.app.ui.components.XpBar
import com.levelup.app.ui.theme.AccentCyan
import com.levelup.app.ui.theme.AccentGold
import com.levelup.app.ui.theme.AccentViolet

@Composable
fun DashboardScreen(
    viewModel: LevelUpViewModel,
    onOpenMission: (MissionEntity) -> Unit,
    onCreate: () -> Unit,
    onSeeAll: () -> Unit,
    onForge: () -> Unit
) {
    val feed by viewModel.dashboard.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeroBlock(feed = feed) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCreate,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("NEW QUEST")
                }
                OutlinedButton(
                    onClick = onForge,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("FORGE")
                }
            }
        }

        section(
            label = "DAILY MISSIONS",
            emptyText = "No daily quests yet. Forge one to start a streak.",
            items = feed.daily
        ) { mission ->
            MissionCard(
                mission = mission,
                onComplete = { viewModel.complete(mission) },
                onClick = { onOpenMission(mission) }
            )
        }

        section(
            label = "MAIN QUESTS",
            emptyText = "No main quests yet. These are your long-arc goals.",
            items = feed.main.filter { !it.completed }.take(3)
        ) { mission ->
            MissionCard(
                mission = mission,
                onComplete = { viewModel.complete(mission) },
                onClick = { onOpenMission(mission) }
            )
        }

        section(
            label = "SIDE QUESTS",
            emptyText = "No side quests yet.",
            items = feed.side.filter { !it.completed }.take(3),
            trailing = {
                TextButton(onClick = onSeeAll) { Text("See all") }
            }
        ) { mission ->
            MissionCard(
                mission = mission,
                onComplete = { viewModel.complete(mission) },
                onClick = { onOpenMission(mission) }
            )
        }
    }
}

@Composable
private fun HeroBlock(feed: DashboardFeed) {
    val totalXp = feed.profile?.totalXp ?: 0L
    val progress = Progression.levelProgress(totalXp)
    val rank = Progression.rankFor(progress.level)
    val completedDaily = feed.daily.count { it.lastCompletedDate == DailyReset.today() }
    val totalDaily = feed.daily.size

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RankBadge(rank = rank)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        feed.profile?.hunterName ?: "Hunter",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Rank ${rank.label} · ${rank.tagline}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                NextRankHint(rank = rank, level = progress.level)
            }
            Spacer(Modifier.height(14.dp))
            XpBar(
                progress = progress.fraction,
                level = progress.level,
                xpIntoLevel = progress.xpIntoLevel,
                xpForNextLevel = progress.xpForNextLevel
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Pill("$completedDaily / $totalDaily today", AccentCyan)
                Pill("${feed.main.count { !it.completed }} main", AccentGold)
                Pill("${feed.side.count { !it.completed }} side", AccentViolet)
            }
        }
    }
}

@Composable
private fun NextRankHint(rank: Rank, level: Int) {
    val nextThreshold = when (rank) {
        Rank.E -> 8
        Rank.D -> 18
        Rank.C -> 30
        Rank.B -> 45
        Rank.A -> 60
        Rank.S -> null
    }
    if (nextThreshold == null) {
        AssistChip(
            onClick = {},
            label = { Text("MAX RANK", style = MaterialTheme.typography.labelMedium) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = AccentGold
            )
        )
    } else {
        AssistChip(
            onClick = {},
            label = {
                Text(
                    "Next rank: L$nextThreshold",
                    style = MaterialTheme.typography.labelMedium
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

// --- LazyColumn helpers -----------------------------------------------------

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    label: String,
    emptyText: String,
    items: List<MissionEntity>,
    trailing: (@Composable () -> Unit)? = null,
    itemContent: @Composable (MissionEntity) -> Unit
) {
    item {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(label, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
    }
    if (items.isEmpty()) {
        item { EmptyCard(emptyText) }
    } else {
        items(items, key = { it.id }) { mission -> itemContent(mission) }
    }
}

@Composable
private fun EmptyCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Suppress("unused")
@Composable
private fun TypeDot(type: MissionType) {
    val color = when (type) {
        MissionType.MAIN -> AccentGold
        MissionType.SIDE -> AccentViolet
        MissionType.DAILY -> AccentCyan
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

@Suppress("unused")
@Composable
private fun RowSpacer() {
    Spacer(modifier = Modifier.height(8.dp))
}
