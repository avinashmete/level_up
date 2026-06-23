package com.levelup.app

import com.levelup.app.ai.QuestSuggester
import com.levelup.app.data.LevelUpRepository
import com.levelup.app.data.db.DriverFactory
import com.levelup.app.data.db.LevelUpDatabase
import com.levelup.app.data.prefs.UserPreferences
import com.russhwolf.settings.ObservableSettings

/**
 * Manual DI container built once per process. Each platform supplies its own
 * [DriverFactory] and [ObservableSettings].
 */
class AppContainer(
    driverFactory: DriverFactory,
    settings: ObservableSettings
) {
    private val database = LevelUpDatabase(driverFactory.createDriver())

    val repository: LevelUpRepository = LevelUpRepository(database)
    val preferences: UserPreferences = UserPreferences(settings)
    val questSuggester: QuestSuggester = QuestSuggester(preferences)
}
