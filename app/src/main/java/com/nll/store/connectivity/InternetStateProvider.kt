package com.nll.store.connectivity

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.nll.store.log.CLog
import com.nll.store.utils.ApiLevel
import com.nll.store.utils.extConnectivityManager
import com.nll.store.utils.extDebounce
import com.nll.store.utils.extTryStartActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


/**

InternetStateProvider.isDeviceOnline()
or

InternetStateProvider.isDeviceOnlineFlow().onEach { isDeviceOnline ->

}.launchIn(lifecycleScope)

 */
object InternetStateProvider {
    private const val logTag = "InternetStateProvider"
    private const val delayedStartUpStatusCheckTimeInMs = 5000L
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
        if (ApiLevel.isQPlus()) {
            context.extTryStartActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
        }
    }

    private fun ensureInternetStateMonitorIsRegistered() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "ensureInternetStateMonitorIsRegistered() -> internetStateMonitor : $internetStateMonitor")
        }
        internetStateMonitor?.ensureRegistered()
    }


    internal fun start(context: Context, skipDelayedStatusCheck: Boolean) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "start -> skipDelayedStatusCheck: $skipDelayedStatusCheck, Start observing Internet state")
        }
        updateDeprecatedIsOnline(context)
        if (CLog.isDebug()) {
            CLog.log(logTag, "start -> deprecatedIsOnline: $deprecatedIsOnline")
        }
        internetStateMonitor = InternetStateMonitor(context.extConnectivityManager()) { state ->

            if (CLog.isDebug()) {
                CLog.log(logTag, "internetStateMonitor -> callBack -> state is now $state")
            }

            /**
             * We may get
             * java.lang.SecurityException: Package android does not belong to 10265
             *
             * Due to https://issuetracker.google.com/issues/175055271
             *
             * So we register a one time worker to re init
             *
             */
            state?.let {
                //Set state
                networkState.value = it
                //Update updateDeprecatedIsOnline to reflect changes so that isDeviceOnline would return current value if and when networkState.value.isDeviceOnline() is false
                updateDeprecatedIsOnline(context)
            }


        }


        if (!skipDelayedStatusCheck) {
            /**
             * Check state of internetStateMonitor after delayedStartUpStatusCheckTimeInMs seconds to make sure it is running due to https://issuetracker.google.com/issues/175055271
             */
            Handler(Looper.getMainLooper()).postDelayed({
                if (internetStateMonitor == null) {
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "start -> Delayed status check for InternetStateMonitor found that  was InternetStateMonitor was NULL. Calling start() again")
                    }
                    start(context, true)
                } else {
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "start -> Delayed status check for InternetStateMonitor calling ensureInternetStateMonitorIsRegistered() to make sure we are observing connectivity changes")
                    }
                    ensureInternetStateMonitorIsRegistered()
                }
            }, delayedStartUpStatusCheckTimeInMs)
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

    fun isConnectedViaWifi(): Boolean {
        val result = if (isDeviceOnline()) {
            internetStateMonitor?.isConnectionUnMetered() ?: false
        } else {
            false
        }

        if (CLog.isDebug()) {
            CLog.log(logTag, "isConnectedViaWifi() -> $result")
        }
        return result
    }

    fun networkStateFlow() = networkState.asStateFlow()

    /**
     * Uses only the latest networkState within provided waitMillis or 5 seconds if no waitMillis provided.
     * Sometimes we get updates quite quickly.
     * For example, putting device to airplane mode triggers network change with intern access true first then triggers onLost withing milliseconds.
     */
    fun networkStateFlowDelayed(waitMillis: Long = 1000) = networkStateFlow().extDebounce(waitMillis)

}