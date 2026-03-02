package com.nll.store.connectivity

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.nll.store.utils.ApiLevel
import com.nll.store.utils.extTryStartActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.transformLatest

object NetworkStateController {
    private val coroutineScope by lazy {  CoroutineScope(Dispatchers.Main + SupervisorJob()) }
    @Volatile
    private var networkStateProvider: NetworkStateProvider? = null
    private fun getOrCreateInstance(context: Context): NetworkStateProvider {
        val local = networkStateProvider
        if (local != null) {
            return local
        }

        return synchronized(this) {
            val local2 = networkStateProvider
            if (local2 != null) {
                local2
            } else {
                val created = NetworkStateProvider(context.applicationContext, coroutineScope)
                networkStateProvider = created
                created
            }
        }
    }

    fun startObserving(context: Context, addDelay: Boolean) = getOrCreateInstance(context).startObserving(addDelay)
    fun ensureCallbackIsRegistered(context: Context) = getOrCreateInstance(context).ensureCallbackIsRegistered()
    fun isConnectedViaWifi(context: Context) = getOrCreateInstance(context).isConnectedViaWifi()
    fun isDeviceOnline() = networkStateProvider?.isDeviceOnline() == true
    fun networkStateFlow(context: Context) = getOrCreateInstance(context).networkStateFlow

    /**
     * Uses only the latest networkState within provided waitMillis or 5 seconds if no waitMillis provided.
     * Sometimes we get updates quite quickly.
     * For example, putting device to airplane mode triggers network change with intern access true firsts then triggers onLost withing milliseconds.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun networkStateFlowDelayed(context: Context, waitMillis: Long = 5_000) =
        getOrCreateInstance(context)
            .networkStateFlow
            .transformLatest { state ->
                delay(waitMillis)
                emit(state)
            }

    fun openQuickInterNetConnectivityMenuIfYouCan(context: Context) {
        if (ApiLevel.isQPlus()) {
            context.extTryStartActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
        }
    }
}
