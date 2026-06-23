package com.levelup.app.platform

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Per-platform short transient message ("Saved", "Failed", etc.). */
interface Toaster {
    fun show(message: String)
}

/**
 * Default in-app implementation that surfaces messages through the Compose Snackbar.
 * Each platform host can swap this for a native Toast/HUD if desired.
 */
class FlowToaster : Toaster {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    override fun show(message: String) {
        _messages.tryEmit(message)
    }
}
