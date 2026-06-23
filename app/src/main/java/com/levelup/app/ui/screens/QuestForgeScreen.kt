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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.levelup.app.ai.QuestSuggestion
import com.levelup.app.ui.LevelUpViewModel
import com.levelup.app.ui.SuggestionUiState
import com.levelup.app.ui.components.Pill
import com.levelup.app.ui.components.SectionLabel
import com.levelup.app.ui.components.difficultyColor
import com.levelup.app.ui.components.statColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestForgeScreen(viewModel: LevelUpViewModel) {
    val state by viewModel.suggestionState.collectAsState()
    val provider by viewModel.aiProvider.collectAsState(initial = "offline")
    val hasKey by viewModel.aiApiKey.collectAsState(initial = "")

    var goal by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "QUEST FORGE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Type a life goal. The forge turns it into a set of quests you can accept.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = goal,
                        onValueChange = { goal = it.take(180) },
                        label = { Text("Goal (e.g. 'Get stronger', 'Learn Japanese')") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(
                            text = if (provider == "openai" && hasKey.isNotBlank()) "AI · ON" else "AI · OFF (offline templates)",
                            color = if (provider == "openai" && hasKey.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { viewModel.generateQuests(goal) },
                            enabled = goal.isNotBlank() && state !is SuggestionUiState.Loading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (state is SuggestionUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("FORGE")
                        }
                    }
                }
            }
        }

        when (val s = state) {
            SuggestionUiState.Idle -> Unit
            SuggestionUiState.Loading -> {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            is SuggestionUiState.Ready -> {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("SUGGESTIONS · ${s.result.source}", modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.generateQuests(s.goal) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                        TextButton(onClick = { viewModel.clearSuggestions() }) {
                            Text("CLEAR")
                        }
                    }
                }
                items(s.result.suggestions) { suggestion ->
                    SuggestionRow(suggestion = suggestion, onAccept = { viewModel.saveSuggestion(it) })
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: QuestSuggestion, onAccept: (QuestSuggestion) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Pill(suggestion.type.name, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Pill(suggestion.stat.displayName, color = statColor(suggestion.stat))
                    Spacer(Modifier.width(6.dp))
                    Pill(suggestion.difficulty.displayName, color = difficultyColor(suggestion.difficulty))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    suggestion.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                if (suggestion.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        suggestion.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "+${suggestion.difficulty.xp} XP · +${suggestion.difficulty.statPoints} ${suggestion.stat.emoji}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(Modifier.width(10.dp))
            IconButton(onClick = { onAccept(suggestion) }) {
                Icon(
                    Icons.Filled.AddCircle,
                    contentDescription = "Accept quest",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
