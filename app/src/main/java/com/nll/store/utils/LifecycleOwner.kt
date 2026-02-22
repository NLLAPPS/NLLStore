package com.nll.store.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Extension function to simplify collecting flows with repeatOnLifecycle.
 * @param state The Lifecycle state to observe. Default is (according to Google examples etc) Lifecycle.State.STARTED
 * @param collectBlock A lambda to define what to collect.
 */
fun <T> LifecycleOwner.extRepeatOnLifecycleCollectLatest(
    flow: Flow<T>, state: Lifecycle.State = Lifecycle.State.STARTED, collectBlock: suspend (T) -> Unit
) {
    lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(state) {
            flow.collectLatest { value ->
                collectBlock(value)
            }
        }
    }
}
