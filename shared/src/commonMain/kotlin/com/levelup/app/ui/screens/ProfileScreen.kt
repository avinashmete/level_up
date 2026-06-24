package com.levelup.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.levelup.app.data.StatType
import com.levelup.app.domain.Progression
import com.levelup.app.ui.LevelUpAppState
import com.levelup.app.ui.components.RankBadge
import com.levelup.app.ui.components.SectionLabel
import com.levelup.app.ui.components.StatChip
import com.levelup.app.ui.components.XpBar
import com.levelup.app.ui.components.statColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(state: LevelUpAppState) {
    val feed by state.dashboard.collectAsState()
    val achievements by state.achievements.collectAsState(initial = emptyList())
    val profile = feed.profile

    var name by remember { mutableStateOf(profile?.hunterName ?: "Hunter") }
    LaunchedEffect(profile?.hunterName) {
        name = profile?.hunterName ?: "Hunter"
    }

    val totalXp = profile?.totalXp ?: 0L
    val progress = Progression.levelProgress(totalXp)
    val rank = Progression.rankFor(progress.level)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RankBadge(rank = rank)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it.take(24) },
                                label = { Text("Hunter name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Rank ${rank.label} · ${rank.tagline}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    XpBar(progress = progress.fraction, level = progress.level, xpIntoLevel = progress.xpIntoLevel, xpForNextLevel = progress.xpForNextLevel)
                    LaunchedEffect(name) {
                        if (name.isNotBlank() && name != profile?.hunterName) {
                            state.rename(name)
                        }
                    }
                }
            }
        }

        item { SectionLabel("STATS") }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("STR", profile?.strength ?: 0, color = statColor(StatType.STRENGTH))
                StatChip("INT", profile?.intelligence ?: 0, color = statColor(StatType.INTELLIGENCE))
                StatChip("DIS", profile?.discipline ?: 0, color = statColor(StatType.DISCIPLINE))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("VIT", profile?.vitality ?: 0, color = statColor(StatType.VITALITY))
                StatChip("SOC", profile?.social ?: 0, color = statColor(StatType.SOCIAL))
                StatChip("DONE", profile?.lifetimeMissionsCompleted ?: 0)
            }
        }

        item { SectionLabel("ACHIEVEMENTS") }
        if (achievements.isEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No achievements yet. Complete a quest to unlock your first one.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(achievements, key = { it.code }) { ach ->
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ach.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text(ach.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
