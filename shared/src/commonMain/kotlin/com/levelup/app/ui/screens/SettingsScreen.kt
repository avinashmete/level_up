package com.levelup.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.levelup.app.platform.BackupBridge
import com.levelup.app.platform.Toaster
import com.levelup.app.ui.LevelUpAppState
import com.levelup.app.ui.components.SectionLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: LevelUpAppState,
    backup: BackupBridge,
    toaster: Toaster
) {
    val provider by state.aiProvider.collectAsState(initial = "offline")
    val apiKey by state.aiApiKey.collectAsState(initial = "")
    val model by state.aiModel.collectAsState(initial = "gpt-4o-mini")

    var keyDraft by remember { mutableStateOf("") }
    LaunchedEffect(apiKey) { keyDraft = apiKey }
    var modelDraft by remember { mutableStateOf(model) }
    LaunchedEffect(model) { modelDraft = model }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(PaddingValues(16.dp)),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("SYSTEM SETTINGS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)

        SectionLabel("AI QUEST GENERATOR")
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("offline" to "Offline", "openai" to "OpenAI").forEach { (value, label) ->
                        FilterChip(
                            selected = provider == value,
                            onClick = { state.setProvider(value) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                if (provider == "openai") {
                    OutlinedTextField(
                        value = keyDraft,
                        onValueChange = { keyDraft = it },
                        label = { Text("OpenAI API key (stored on device)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColorsCompat()
                    )
                    OutlinedTextField(
                        value = modelDraft,
                        onValueChange = { modelDraft = it },
                        label = { Text("Model") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColorsCompat()
                    )
                    Button(
                        onClick = {
                            state.setApiKey(keyDraft.trim())
                            state.setModel(modelDraft.trim().ifBlank { "gpt-4o-mini" })
                            toaster.show("Saved")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("SAVE KEY")
                    }
                } else {
                    Text(
                        "Offline mode uses a built-in template generator. No data leaves your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SectionLabel("BACKUP")
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Save your profile and quests to a JSON file you control, or restore from one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                val json = runCatching { state.exportSnapshotJson() }.getOrNull()
                                if (json == null) {
                                    toaster.show("Export failed")
                                    return@launch
                                }
                                backup.exportJson(
                                    suggestedName = "levelup-backup.json",
                                    json = json
                                ) { outcome ->
                                    when (outcome) {
                                        BackupBridge.Outcome.Success -> toaster.show("Backup saved")
                                        BackupBridge.Outcome.Cancelled -> Unit
                                        BackupBridge.Outcome.Failure -> toaster.show("Export failed")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("EXPORT")
                    }
                    OutlinedButton(
                        onClick = {
                            backup.importJson { outcome, raw ->
                                when (outcome) {
                                    BackupBridge.Outcome.Success -> {
                                        if (raw != null) {
                                            scope.launch {
                                                state.importSnapshotJson(raw)
                                                    .onSuccess { toaster.show("Backup restored") }
                                                    .onFailure { toaster.show("Import failed: ${it.message}") }
                                            }
                                        } else toaster.show("Import failed: empty file")
                                    }
                                    BackupBridge.Outcome.Cancelled -> Unit
                                    BackupBridge.Outcome.Failure -> toaster.show("Import failed")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Upload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("IMPORT")
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "LevelUp v0.2 · Local-first. Compose Multiplatform (Android + iOS).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun textFieldColorsCompat() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
)
