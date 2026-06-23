package com.levelup.app.data

import android.content.Context
import androidx.room.Room
import com.levelup.app.ai.QuestSuggester
import com.levelup.app.data.prefs.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual DI container. Holds singletons that live for the process lifetime.
 * Built once in [com.levelup.app.LevelUpApplication.onCreate].
 */
class LevelUpContainer(context: Context) {

    private val applicationContext = context.applicationContext

    private val db: LevelUpDatabase = Room.databaseBuilder(
        applicationContext,
        LevelUpDatabase::class.java,
        "levelup.db"
    ).fallbackToDestructiveMigration().build()

    val repository: LevelUpRepository = LevelUpRepository(
        missionDao = db.missionDao(),
        completionDao = db.completionDao(),
        profileDao = db.profileDao(),
        achievementDao = db.achievementDao()
    )

    val preferences: UserPreferences = UserPreferences(applicationContext)

    val questSuggester: QuestSuggester = QuestSuggester(preferences)

    val context: Context get() = applicationContext

    init {
        // Bootstrap profile + daily reset so the UI always has something to render.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            repository.ensureProfile()
            repository.runDailyResetIfNeeded()
        }
    }
}
