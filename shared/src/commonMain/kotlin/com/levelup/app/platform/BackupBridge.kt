package com.levelup.app.platform

/**
 * Per-platform "save this JSON to a user-selected location" and
 * "load JSON from a user-selected location".
 *
 * The Compose layer prepares the JSON string itself (via [com.levelup.app.ui.LevelUpAppState.exportSnapshotJson]),
 * so this bridge only deals with bytes + native pickers.
 *
 * Android wires this to the Storage Access Framework via Activity result launchers.
 * iOS uses UIDocumentPicker.
 */
interface BackupBridge {
    fun exportJson(suggestedName: String, json: String, onResult: (Outcome) -> Unit)
    fun importJson(onResult: (Outcome, String?) -> Unit)

    enum class Outcome { Success, Cancelled, Failure }
}
