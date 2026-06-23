package com.levelup.app.android

import android.app.Application
import com.levelup.app.AndroidPlatform
import com.levelup.app.AppContainer

class LevelUpApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AndroidPlatform.createContainer(this)
    }
}
