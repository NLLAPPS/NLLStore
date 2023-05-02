package com.nll.store.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

object CoroutineScopeFactory {

    fun create(context: CoroutineContext): CoroutineScope = ContextScope(if (context[Job] != null) context else context + Job())

    internal class ContextScope(context: CoroutineContext) : CoroutineScope {
        override val coroutineContext = context
        override fun toString(): String = "CoroutineScopeFactory(coroutineContext=$coroutineContext)"
    }
}