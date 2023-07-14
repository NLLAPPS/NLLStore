package com.nll.store.debug

sealed class DebugLogServiceCommand {

    data object Stop : DebugLogServiceCommand()
    data object Save : DebugLogServiceCommand()
    data object Clear : DebugLogServiceCommand()
}