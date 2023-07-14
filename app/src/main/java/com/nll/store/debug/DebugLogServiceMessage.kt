package com.nll.store.debug

import java.util.LinkedList

sealed class DebugLogServiceMessage {

    data class Started(val currentLogs: LinkedList<String>) : DebugLogServiceMessage()
    data object Stopped : DebugLogServiceMessage()
    data class Saved(val success: Boolean, val path: String?) : DebugLogServiceMessage()
}