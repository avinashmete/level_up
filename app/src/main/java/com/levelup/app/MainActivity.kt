package com.levelup.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.levelup.app.ui.LevelUpApp
import com.levelup.app.ui.theme.LevelUpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LevelUpApplication).container
        setContent {
            LevelUpTheme {
                LevelUpApp(container = container)
            }
        }
    }
}
