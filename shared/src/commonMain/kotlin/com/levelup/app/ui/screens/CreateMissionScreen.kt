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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.levelup.app.data.Difficulty
import com.levelup.app.data.Mission
import com.levelup.app.data.MissionType
import com.levelup.app.data.StatType
import com.levelup.app.ui.LevelUpAppState
import com.levelup.app.ui.components.SectionLabel
import com.levelup.app.ui.components.difficultyColor
import com.levelup.app.ui.components.statColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMissionScreen(
    state: LevelUpAppState,
    existing: Mission? = null,
    onDone: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var description by rememberSaveable { mutableStateOf(existing?.description ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: MissionType.DAILY) }
    var stat by remember { mutableStateOf(existing?.stat ?: StatType.DISCIPLINE) }
    var difficulty by remember { mutableStateOf(existing?.difficulty ?: Difficulty.NORMAL) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "NEW QUEST" else "EDIT QUEST", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(PaddingValues(horizontal = 16.dp)),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(400) },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                colors = textFieldColors()
            )

            SectionLabel("TYPE")
            ChipRow(
                items = MissionType.entries,
                selected = type,
                labelOf = { it.name },
                colorOf = {
                    when (it) {
                        MissionType.MAIN -> MaterialTheme.colorScheme.tertiary
                        MissionType.SIDE -> MaterialTheme.colorScheme.secondary
                        MissionType.DAILY -> MaterialTheme.colorScheme.primary
                    }
                },
                onSelect = { type = it }
            )

            SectionLabel("STAT")
            ChipRow(
                items = StatType.entries,
                selected = stat,
                labelOf = { "${it.emoji} · ${it.displayName}" },
                colorOf = { statColor(it) },
                onSelect = { stat = it }
            )

            SectionLabel("DIFFICULTY")
            ChipRow(
                items = Difficulty.entries,
                selected = difficulty,
                labelOf = { "${it.displayName} (+${it.xp} XP)" },
                colorOf = { difficultyColor(it) },
                onSelect = { difficulty = it }
            )

            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Reward", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "+${difficulty.xp} XP · +${difficulty.statPoints} ${stat.emoji}",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    state.saveMission(existing, title, description, type, stat, difficulty)
                    onDone()
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("ACCEPT QUEST")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChipRow(
    items: List<T>,
    selected: T,
    labelOf: (T) -> String,
    colorOf: @Composable (T) -> Color,
    onSelect: (T) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { item ->
            val isSelected = item == selected
            val color = colorOf(item)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(item) },
                label = { Text(labelOf(item)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = color.copy(alpha = 0.20f),
                    selectedLabelColor = color
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun textFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
)
