package com.levelup.app.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.levelup.app.platform.Toaster
import com.levelup.app.ui.LevelUpApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LevelUpApplication).container
        val backupBridge = AndroidBackupBridge(this)
        val toaster: Toaster = object : Toaster {
            override fun show(message: String) {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
        setContent {
            LevelUpApp(
                container = container,
                backupBridge = backupBridge,
                toaster = toaster
            )
        }
    }
}
