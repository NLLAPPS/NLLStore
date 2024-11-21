package com.nll.store.connectivity

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.nll.store.log.CLog
import com.nll.store.utils.ApiLevel

internal class InternetStateMonitor(private val connectivityManager: ConnectivityManager?, private val callBack: (NetworkState?) -> Unit) : ConnectivityManager.NetworkCallback() {
    private val logTag = "InternetStateMonitor"
    private var callbacksRegistered = false
    private var currentNetworkState: NetworkState? = connectivityManager?.extActiveNetworkState

    init {
        if (CLog.isDebug()) {
            CLog.log(logTag, "init")
        }

        //Emit initial status before ve register callbacks. Because is initial status we get from connectivityManager?.extActiveNetworkState is same as we get on call back
        //No emit would happen due to checks we make at updateNetworkState()
        callBack(currentNetworkState)

        tryRegister()
    }

    /*
        Due to https://issuetracker.google.com/issues/175055271
    */
    fun ensureRegistered() {
        if (callbacksRegistered) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "ensureRegistered() -> callbacksRegistered was True. Ignoring request")
            }
        } else {
            if (CLog.isDebug()) {
                CLog.log(logTag, "ensureRegistered() -> callbacksRegistered was False. Trying to register again")
            }
            tryRegister()
        }
    }

    private fun tryRegister() {
        /*
           Due to https://issuetracker.google.com/issues/175055271
        */
        try {
            /**
             * Interesting fact.
             * Using object : ConnectivityManager.NetworkCallback as a variable of this class caused
             * registerDefaultNetworkCallback to complain that callback was null
             */
            connectivityManager?.registerDefaultNetworkCallback(this)
            callbacksRegistered = true
            if (CLog.isDebug()) {
                CLog.log(logTag, "tryRegister() -> Callback registered")
            }
        } catch (e: Exception) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "tryRegister() -> Unable to register callback")
            }
            CLog.logPrintStackTrace(e)

        }
    }

    fun isConnectionUnMetered(): Boolean {
        val currentNetworkStateIsUnMetered = currentNetworkState?.isConnectionUnMetered()
        if (CLog.isDebug()) {
            CLog.log(logTag, "isConnectionUnMetered() -> currentNetworkStateIsUnMetered: $currentNetworkStateIsUnMetered")
        }

        return if (currentNetworkStateIsUnMetered != null) {
            currentNetworkStateIsUnMetered
        } else {
            val extIsActiveConnectionUnMetered = connectivityManager?.extIsActiveConnectionUnMetered ?: false
            if (CLog.isDebug()) {
                CLog.log(logTag, "isConnectionUnMetered() -> extIsActiveConnectionUnMetered: $extIsActiveConnectionUnMetered")
            }
            extIsActiveConnectionUnMetered
        }
    }

    override fun onLost(network: Network) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "networkCallback() -> onLost() -> network: $network")
        }
        updateNetworkState()

    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        /*if (CLog.isDebug()) {
            CLog.log(logTag, "networkCallback() -> onCapabilitiesChanged() -> networkCapabilities: $networkCapabilities")
        }*/
        updateNetworkState()

    }

    private fun updateNetworkState() {

        connectivityManager?.let { manager ->
            val networkState = manager.extActiveNetworkState
            if (networkState != currentNetworkState) {
                if (CLog.isDebug()) {
                    CLog.log(logTag, "updateNetworkState() -> Posting update.  New state: $networkState, oldState: $currentNetworkState")
                }
                currentNetworkState = networkState
                callBack(networkState)

            }
        }
    }


    private val ConnectivityManager.extActiveNetworkState: NetworkState?
        get() {
            return try {
                val networkCapability = getNetworkCapabilities(activeNetwork)
                if (CLog.isDebug()) {
                    CLog.log(logTag, "extActiveNetworkState -> networkCapability: $networkCapability")
                }
                val hasInternetCapability = networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
                val hasNotRestrictedInternetCapability = networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED) ?: false
                val canConnectToTheInternet = hasInternetCapability && hasNotRestrictedInternetCapability
                val isUnMetered = networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ?: true
                val isNotRoaming = if (ApiLevel.isPiePlus()) {
                    networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING) ?: true
                } else {
                    true
                }
                val networkHandle = activeNetwork?.networkHandle ?: 0L
                NetworkState(hasInternetCapability = canConnectToTheInternet, isMetered = isUnMetered.not(), isRoaming = isNotRoaming.not(), networkHandle = networkHandle)
            } catch (e: Exception) {
                CLog.logPrintStackTrace(e)
                null
            }
        }

    private val ConnectivityManager.extIsActiveConnectionUnMetered: Boolean
        get() {
            return isActiveNetworkMetered.not()

        }
}