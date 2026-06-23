package com.levelup.app.ui

import com.levelup.app.ai.QuestSuggester
import com.levelup.app.ai.QuestSuggestion
import com.levelup.app.ai.SuggestionResult
import com.levelup.app.data.CompletionResult
import com.levelup.app.data.DashboardFeed
import com.levelup.app.data.Difficulty
import com.levelup.app.data.LevelUpRepository
import com.levelup.app.data.Mission
import com.levelup.app.data.MissionType
import com.levelup.app.data.StatType
import com.levelup.app.data.backup.Backup
import com.levelup.app.data.prefs.UserPreferences
import com.levelup.app.domain.DailyReset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-wide state holder. Plays the role a ViewModel would on Android-only,
 * but stays platform-agnostic so iOS can use it too.
 *
 * Lifetime is the Compose root: built once in [com.levelup.app.ui.LevelUpApp] and
 * remembered across recompositions.
 */
class LevelUpAppState(
    private val scope: CoroutineScope,
    private val repository: LevelUpRepository,
    private val suggester: QuestSuggester,
    private val preferences: UserPreferences
) {

    init {
        scope.launch {
            repository.ensureProfile()
            repository.runDailyResetIfNeeded()
        }
    }

    val dashboard: StateFlow<DashboardFeed> = repository.dashboardFeed()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardFeed(null, emptyList(), emptyList(), emptyList())
        )

    val achievements = repository.achievements

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    private val _suggestionState = MutableStateFlow<SuggestionUiState>(SuggestionUiState.Idle)
    val suggestionState: StateFlow<SuggestionUiState> = _suggestionState.asStateFlow()

    val aiProvider = preferences.aiProvider
    val aiApiKey = preferences.aiApiKey
    val aiModel = preferences.aiModel

    fun rename(name: String) = scope.launch { repository.renameHunter(name) }

    fun complete(mission: Mission) = scope.launch {
        val result = repository.completeMission(mission.id) ?: return@launch
        _events.emit(UiEvent.Completed(result))
    }

    fun saveMission(
        existing: Mission?,
        title: String,
        description: String,
        type: MissionType,
        stat: StatType,
        difficulty: Difficulty
    ) = scope.launch {
        val now = DailyReset.nowMillis()
        val entity = (existing ?: Mission(
            title = "",
            type = type,
            stat = stat,
            difficulty = difficulty,
            xpReward = difficulty.xp,
            statReward = difficulty.statPoints,
            createdAt = now
        )).copy(
            title = title.trim().ifBlank { "Untitled quest" },
            description = description.trim(),
            type = type,
            stat = stat,
            difficulty = difficulty,
            xpReward = difficulty.xp,
            statReward = difficulty.statPoints
        )
        repository.saveMission(entity)
    }

    fun saveSuggestion(suggestion: QuestSuggestion) = scope.launch {
        repository.saveMission(
            Mission(
                title = suggestion.title,
                description = suggestion.description,
                type = suggestion.type,
                stat = suggestion.stat,
                difficulty = suggestion.difficulty,
                xpReward = suggestion.difficulty.xp,
                statReward = suggestion.difficulty.statPoints,
                createdAt = DailyReset.nowMillis()
            )
        )
    }

    fun archive(missionId: Long) = scope.launch { repository.archive(missionId) }
    fun delete(missionId: Long) = scope.launch { repository.deleteMission(missionId) }

    fun setProvider(provider: String) = preferences.setAiProvider(provider)
    fun setApiKey(key: String) = preferences.setAiApiKey(key)
    fun setModel(model: String) = preferences.setAiModel(model)

    fun generateQuests(goal: String, count: Int = 5) = scope.launch {
        if (goal.isBlank()) return@launch
        _suggestionState.value = SuggestionUiState.Loading
        val result: SuggestionResult = suggester.suggest(goal.trim(), count)
        _suggestionState.value = SuggestionUiState.Ready(goal.trim(), result)
    }

    fun clearSuggestions() {
        _suggestionState.value = SuggestionUiState.Idle
    }

    suspend fun exportSnapshotJson(): String = Backup.encode(repository.snapshot())

    suspend fun importSnapshotJson(raw: String): Result<Unit> = runCatching {
        val snapshot = Backup.decode(raw)
        repository.restore(snapshot)
    }
}

sealed interface SuggestionUiState {
    data object Idle : SuggestionUiState
    data object Loading : SuggestionUiState
    data class Ready(val goal: String, val result: SuggestionResult) : SuggestionUiState
}

sealed interface UiEvent {
    data class Completed(val result: CompletionResult) : UiEvent
}
