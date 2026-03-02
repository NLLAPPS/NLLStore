package com.nll.store.connectivity

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull

internal class NetworkStateProvider(context: Context, scope: CoroutineScope) {
    private val observer = NetworkCallbackFlowObserver(context.applicationContext, scope)
    val networkStateFlow = observer.networkStateFlow.filterNotNull()
    fun startObserving(addDelay: Boolean) = observer.startObserving(addDelay)
    fun isDeviceOnline() = observer.isDeviceOnline()
    fun isConnectedViaWifi() = observer.isConnectedViaWifi()
    fun ensureCallbackIsRegistered() = observer.ensureRegistered()
}