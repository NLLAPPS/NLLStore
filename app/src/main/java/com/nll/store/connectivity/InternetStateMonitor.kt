package com.nll.store.connectivity

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.nll.asr.network.connectivity.NetworkState
import com.nll.store.log.CLog

internal class InternetStateMonitor(private val connectivityManager: ConnectivityManager?, private val callBack: (NetworkState) -> Unit) : ConnectivityManager.NetworkCallback() {
    private val logTag = "InternetStateMonitor"
    private var callbacksRegistered = false
    private var currentNetworkState: NetworkState? = connectivityManager?.extActiveNetworkState

    init {
        if (CLog.isDebug()) {
            CLog.log(logTag, "init")
        }
        //Emit initial status before ve register callbacks. Because is initial status we get from connectivityManager?.extActiveNetworkState is same as we get on call back
        //No emit would happen due to checks we make at updateNetworkState()
        currentNetworkState?.let {
            callBack(it)
        }

        tryRegister()
    }


    private fun tryRegister() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "tryRegister()")
        }
        try {
            /**
             * Interesting fact.
             * Using object : ConnectivityManager.NetworkCallback as a variable of this class caused
             * registerDefaultNetworkCallback to complain that callback was null
             */
            connectivityManager?.registerDefaultNetworkCallback(this)
            callbacksRegistered = true
        } catch (e: Exception) {
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

    private val ConnectivityManager.extActiveNetworkState: NetworkState
        get() {
            val networkCapability = getNetworkCapabilities(activeNetwork)
            /*if (CLog.isDebug()) {
                CLog.log(logTag, "extActiveNetworkState -> networkCapability: $networkCapability")
            }*/
            val hasInternetCapability = networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
            val hasNotRestrictedInternetCapability = networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED) ?: false
            val canConnectToTheInternet = hasInternetCapability && hasNotRestrictedInternetCapability
            val isUnMetered = networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ?: true
            val isNotRoaming =  networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING) ?: true
            val networkHandle = activeNetwork?.networkHandle ?: 0L

            return NetworkState(canConnectToTheInternet, isUnMetered.not(), isNotRoaming.not(), networkHandle)

        }

    private val ConnectivityManager.extIsActiveConnectionUnMetered: Boolean
        get() {
            return isActiveNetworkMetered.not()

        }
}