package com.nll.store.connectivity

import android.content.Context
import com.nll.cb.network.connectivity.NetworkState
import com.nll.store.log.CLog
import com.nll.store.utils.extConnectivityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

internal class NetworkCallbackFlowObserver(private val context: Context, private val scope: CoroutineScope) {
    private val logTag = "NetworkCallbackFlowObserver"
    private var observerJob: Job? = null
    private val connectivityManager = requireNotNull(context.extConnectivityManager()) { "ConnectivityManager not available" }

    /**
     * Getting networkState connectivity takes a little bit time and some functions use isDeviceOnline() as soon as app started.
     * This is the best way to make sure we get real connectivity result without waiting for networkState
     */
    private var deprecatedIsOnline = false
    private val _networkState = MutableStateFlow<NetworkState?>(null)
    val networkStateFlow = _networkState.asStateFlow()


    init {
        if (CLog.isDebug()) {
            CLog.log(logTag, "init()")
        }
        updateDeprecatedIsOnline()
    }

    fun isDeviceOnline(): Boolean {
        val result = networkStateFlow.value?.isDeviceOnline ?: deprecatedIsOnline
        if (CLog.isDebug()) {
            CLog.log(logTag, "isDeviceOnline() -> result: $result, networkStateFlow.value?.isDeviceOnline: ${networkStateFlow.value?.isDeviceOnline}, deprecatedIsOnline: $deprecatedIsOnline")
        }
        return result
    }

    fun isConnectedViaWifi(): Boolean {
        val result = if (isDeviceOnline()) {
            networkStateFlow.value?.isMetered?.not()
                ?: connectivityManager.isActiveNetworkMetered.not()
        } else {
            false
        }
        if (CLog.isDebug()) {
            CLog.log(logTag, "isConnectedViaWifi() -> result: $result, networkStateFlow.value?.isMetered: ${networkStateFlow.value?.isMetered}, connectivityManager.isActiveNetworkMetered: ${connectivityManager.isActiveNetworkMetered}")
        }
        return result
    }

    fun startObserving(addDelay: Boolean) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "startObserving()")
        }
        restart(addDelay)
    }

    /*
        Due to https://issuetracker.google.com/issues/175055271
    */
    fun ensureRegistered() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "ensureRegistered()")
        }
        restart(false)
    }

    fun stopObserving() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "stopObserving()")
        }
        observerJob?.cancel()
        observerJob = null
    }

    private fun restart(addDelay: Boolean) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "restart()")
        }

        observerJob?.cancel()
        observerJob = scope.launch {

            if (addDelay) {
                /**
                 * We have to delay for a bit.
                 * https://issuetracker.google.com/issues/175055271
                 * This delay may cause app not to recognise connectivity initially but there is no other way
                 *
                 * It is fixed in S. See https://android-review.googlesource.com/c/platform/frameworks/base/+/1758029
                 *
                 * We can remove delay after/if we set S as minimum.
                 */
                delay(5.seconds)
            }

            NetworkCallbackFlow(connectivityManager)
                .observe()
                .collect { state ->
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "observe() -> state: $state")
                    }

                    withContext(Dispatchers.Main) {
                        _networkState.value = state
                    }

                    updateDeprecatedIsOnline()

                    if (state == null) {
                        /**
                         * We may get
                         * java.lang.SecurityException: Package android does not belong to 10265
                         *
                         * Due to https://issuetracker.google.com/issues/175055271
                         *
                         * We should register a one time worker to re init
                         *
                         */
                        //InternetStateMonitorWorker.enqueueOneShotWorker(context)
                    }
                }
        }
    }

    @Suppress("DEPRECATION")
    private fun updateDeprecatedIsOnline() {
        deprecatedIsOnline = connectivityManager.activeNetworkInfo?.isConnected ?: false
        /*if (CLog.isDebug()) {
            CLog.log(logTag, "updateDeprecatedIsOnline -> deprecatedIsOnline: ${deprecatedIsOnline}")
        }*/
    }
}