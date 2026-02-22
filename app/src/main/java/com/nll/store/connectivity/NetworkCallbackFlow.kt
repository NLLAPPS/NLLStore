package com.nll.store.connectivity

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.nll.cb.network.connectivity.NetworkState
import com.nll.store.log.CLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal class NetworkCallbackFlow(private val connectivityManager: ConnectivityManager) {
    private val logTag = "NetworkCallbackFlow"

    fun observe(): Flow<NetworkState?> = callbackFlow {
        var delayedLostJob: Job? = null
        fun emitState() {
            val networkState = NetworkState.Companion.networkStateForActiveNetwork(connectivityManager)
            /*if (CLog.isDebug()) {
                CLog.log(logTag, "emitState() -> networkState: $networkState")
            }*/
            trySend(networkState)
        }

        fun scheduleDelayedLost() {
            if (CLog.isDebug()) {
                CLog.log(logTag, "scheduleDelayedLost()")
            }
            delayedLostJob?.cancel()
            delayedLostJob = launch {
                /**
                 * We have to delay for a bit.
                 * https://issuetracker.google.com/issues/175055271
                 * This delay may cause app to not recognise connectivity initially but there is no other way
                 *
                 * It is fixed in S. See https://android-review.googlesource.com/c/platform/frameworks/base/+/1758029
                 *
                 * We can remove delay after/if we set S as minimum.
                 */
                delay(500)
                emitState()
            }
        }


        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                /*if (CLog.isDebug()) {
                    CLog.log(logTag, "onAvailable() -> network: $network")
                }*/
                delayedLostJob?.cancel()
                emitState()
            }

            /**
             * There seems to be a small delay when Android switching networks, especially if both Wi-Fi and mobile data goes offline at the same time
             * Here we introduce a small delay to compensate.
             */
            override fun onLost(network: Network) {
                /*if (CLog.isDebug()) {
                    CLog.log(logTag, "onLost() -> network: $network")
                }*/
                scheduleDelayedLost()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                /*if (CLog.isDebug()) {
                    CLog.log(logTag, "onCapabilitiesChanged() -> network: $network")
                }*/
                emitState()
            }
        }

        emitState()


        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            CLog.logPrintStackTrace(e)
            trySend(null)
            close()
        }

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }.distinctUntilChanged()
}