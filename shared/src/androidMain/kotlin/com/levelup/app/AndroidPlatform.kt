package com.levelup.app

import android.content.Context
import com.levelup.app.data.db.DriverFactory
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Factory used by the Android app module to spin up the shared container.
 * Uses SharedPreferences-backed settings so reads/writes are synchronous and
 * play nicely with the Flow wrappers in [com.levelup.app.data.prefs.UserPreferences].
 */
object AndroidPlatform {
    fun createContainer(context: Context): AppContainer {
        val prefs = context.getSharedPreferences("levelup_prefs", Context.MODE_PRIVATE)
        return AppContainer(
            driverFactory = DriverFactory(context.applicationContext),
            settings = SharedPreferencesSettings(prefs)
        )
    }
}
