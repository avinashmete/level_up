package com.levelup.app

import androidx.compose.ui.window.ComposeUIViewController
import com.levelup.app.platform.BackupBridge
import com.levelup.app.ui.LevelUpApp
import com.russhwolf.settings.NSUserDefaultsSettings
import com.levelup.app.data.db.DriverFactory
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController

/**
 * Entry point Swift calls to obtain the root UIKit view controller hosting the
 * shared Compose UI. iOS-side wires the [BackupBridge] (UIDocumentPicker).
 */
@Suppress("FunctionName")
fun MainViewController(
    backupBridge: BackupBridge
): UIViewController {
    val container = AppContainer(
        driverFactory = DriverFactory(),
        settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    )
    return ComposeUIViewController {
        LevelUpApp(
            container = container,
            backupBridge = backupBridge
        )
    }
}
