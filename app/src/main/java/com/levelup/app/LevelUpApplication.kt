package com.levelup.app

import android.app.Application
import com.levelup.app.data.LevelUpContainer

class LevelUpApplication : Application() {

    lateinit var container: LevelUpContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = LevelUpContainer(this)
    }
}
