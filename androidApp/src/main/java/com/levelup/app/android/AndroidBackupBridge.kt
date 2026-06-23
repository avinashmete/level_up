package com.levelup.app.android

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.levelup.app.platform.BackupBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bridges [BackupBridge] to the Android Storage Access Framework. Activity Result
 * launchers are registered while the host activity is in CREATED state so they
 * survive configuration changes.
 */
class AndroidBackupBridge(private val activity: ComponentActivity) : BackupBridge {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var pendingExport: ((BackupBridge.Outcome) -> Unit)? = null
    private var pendingImport: ((BackupBridge.Outcome, String?) -> Unit)? = null
    private var pendingJson: String? = null

    private val createDocLauncher = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val cb = pendingExport ?: return@registerForActivityResult
        val payload = pendingJson
        pendingExport = null
        pendingJson = null
        if (uri == null || payload == null) {
            cb(BackupBridge.Outcome.Cancelled)
            return@registerForActivityResult
        }
        ioScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    activity.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(payload.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open output stream")
                }
            }.fold(
                onSuccess = { activity.runOnUiThread { cb(BackupBridge.Outcome.Success) } },
                onFailure = { activity.runOnUiThread { cb(BackupBridge.Outcome.Failure) } }
            )
        }
    }

    private val openDocLauncher = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val cb = pendingImport ?: return@registerForActivityResult
        pendingImport = null
        if (uri == null) {
            cb(BackupBridge.Outcome.Cancelled, null)
            return@registerForActivityResult
        }
        ioScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    activity.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    } ?: error("Could not open input stream")
                }
            }.fold(
                onSuccess = { raw -> activity.runOnUiThread { cb(BackupBridge.Outcome.Success, raw) } },
                onFailure = { activity.runOnUiThread { cb(BackupBridge.Outcome.Failure, null) } }
            )
        }
    }

    override fun exportJson(
        suggestedName: String,
        json: String,
        onResult: (BackupBridge.Outcome) -> Unit
    ) {
        pendingExport = onResult
        pendingJson = json
        createDocLauncher.launch(suggestedName)
    }

    override fun importJson(onResult: (BackupBridge.Outcome, String?) -> Unit) {
        pendingImport = onResult
        openDocLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
    }
}
