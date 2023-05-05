package com.nll.store.connectivity

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.nll.store.log.CLog
import com.nll.store.utils.extConnectivityManager
import com.nll.store.utils.extTryStartActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow


/**
 * TODO We have a bug read below
 * We have a bug with this class. Cannot reproduce reliably but seems to happens when changing between wifi and mobile data.
 * We miss the latest state and get stuck on offline (NetworkState#hasInternetCapability=false)
 */
object InternetStateProvider {
    private const val logTag = "InternetStateProvider"
    private var internetStateMonitor: InternetStateMonitor? = null
    private var networkState = MutableStateFlow(NetworkState(hasInternetCapability = false, isMetered = false, isRoaming = false, 0L))

    /**
     * Getting networkState connectivity takes a little bit time and some functions use isDeviceOnline() as soon as app started.
     * This is the best way to make sure we get real connectivity result without waiting for networkState
     */
    private var deprecatedIsOnline = false

    @Suppress("DEPRECATION")
    fun updateDeprecatedIsOnline(context: Context) {
        deprecatedIsOnline = context.extConnectivityManager()?.activeNetworkInfo != null
        if (CLog.isDebug()) {
            CLog.log(logTag, "updateDeprecatedIsOnline -> deprecatedIsOnline: $deprecatedIsOnline")
        }
    }

    fun openQuickInterNetConnectivityMenuIfYouCan(context: Context) {
        context.extTryStartActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
    }


     fun start(context: Context) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "start -> Start observing Internet state")
        }
        updateDeprecatedIsOnline(context)
        if (CLog.isDebug()) {
            CLog.log(logTag, "start -> deprecatedIsOnline: $deprecatedIsOnline")
        }
        internetStateMonitor = InternetStateMonitor(context.extConnectivityManager()) { state ->

            if (CLog.isDebug()) {
                CLog.log(logTag, "internetStateMonitor -> callBack -> state is now $state")
            }
            networkState.value = state
        }
    }

    fun isDeviceOnline(): Boolean {
        /**
         * Getting networkState connectivity takes a little bit time and some functions use isDeviceOnline() as soon as app started.
         * This is the best way to make sure we get real connectivity result without waiting for networkState
         */
        val result = if (networkState.value.isDeviceOnline()) {
            true
        } else {
            deprecatedIsOnline
        }
        if (CLog.isDebug()) {
            CLog.log(logTag, "isDeviceOnline() -> $result")
        }
        return result
    }

    fun networkStateFlow() = networkState.asSharedFlow()
}