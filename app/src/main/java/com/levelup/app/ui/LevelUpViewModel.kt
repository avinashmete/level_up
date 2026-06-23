package com.levelup.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.levelup.app.ai.QuestSuggester
import com.levelup.app.ai.SuggestionResult
import com.levelup.app.data.CompletionResult
import com.levelup.app.data.DashboardFeed
import com.levelup.app.data.Difficulty
import com.levelup.app.data.LevelUpContainer
import com.levelup.app.data.LevelUpRepository
import com.levelup.app.data.MissionEntity
import com.levelup.app.data.MissionType
import com.levelup.app.data.StatType
import com.levelup.app.data.backup.Backup
import com.levelup.app.data.prefs.UserPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Single ViewModel for the whole app. Small surface area, easier to wire than
 * a per-screen mesh and the data set is tiny.
 */
class LevelUpViewModel(
    private val repository: LevelUpRepository,
    private val suggester: QuestSuggester,
    private val preferences: UserPreferences
) : ViewModel() {

    val dashboard: StateFlow<DashboardFeed> = repository.dashboardFeed()
        .stateIn(
            scope = viewModelScope,
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

    fun rename(name: String) = viewModelScope.launch {
        repository.renameHunter(name)
    }

    fun complete(mission: MissionEntity) = viewModelScope.launch {
        val result = repository.completeMission(mission.id) ?: return@launch
        _events.emit(UiEvent.Completed(result))
    }

    fun saveMission(
        existing: MissionEntity?,
        title: String,
        description: String,
        type: MissionType,
        stat: StatType,
        difficulty: Difficulty
    ) = viewModelScope.launch {
        val entity = (existing ?: MissionEntity(
            title = "",
            type = type.name,
            stat = stat.name,
            difficulty = difficulty.name,
            xpReward = difficulty.xp,
            statReward = difficulty.statPoints
        )).copy(
            title = title.trim().ifBlank { "Untitled quest" },
            description = description.trim(),
            type = type.name,
            stat = stat.name,
            difficulty = difficulty.name,
            xpReward = difficulty.xp,
            statReward = difficulty.statPoints
        )
        repository.saveMission(entity)
    }

    fun saveSuggestion(suggestion: com.levelup.app.ai.QuestSuggestion) = viewModelScope.launch {
        repository.saveMission(
            MissionEntity(
                title = suggestion.title,
                description = suggestion.description,
                type = suggestion.type.name,
                stat = suggestion.stat.name,
                difficulty = suggestion.difficulty.name,
                xpReward = suggestion.difficulty.xp,
                statReward = suggestion.difficulty.statPoints
            )
        )
    }

    fun archive(missionId: Long) = viewModelScope.launch {
        repository.archive(missionId)
    }

    fun delete(missionId: Long) = viewModelScope.launch {
        repository.deleteMission(missionId)
    }

    fun setProvider(provider: String) = viewModelScope.launch { preferences.setAiProvider(provider) }
    fun setApiKey(key: String) = viewModelScope.launch { preferences.setAiApiKey(key) }
    fun setModel(model: String) = viewModelScope.launch { preferences.setAiModel(model) }

    fun generateQuests(goal: String, count: Int = 5) = viewModelScope.launch {
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

    companion object {
        fun factory(container: LevelUpContainer) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LevelUpViewModel(
                    repository = container.repository,
                    suggester = container.questSuggester,
                    preferences = container.preferences
                ) as T
            }
        }
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
