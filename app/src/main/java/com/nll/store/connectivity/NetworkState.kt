package com.nll.cb.network.connectivity

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.nll.store.log.CLog
import com.nll.store.utils.ApiLevel

data class NetworkState(

    val isDeviceOnline: Boolean,
    /**
     * Determines if the network is metered. Such as Mobile Data
     * It also allows us to inform observers that actual data connection type changed (from wifi to mobile and vice versa) which is useful on SIP connections
     */
    val isMetered: Boolean,

    val isRoaming: Boolean,

    /**
     * To make sure we broadcast changes even if hasInternetCapability/isMetered not changed. Perhaps user has 2 sim cards or connected to vpn. networkHandle changes for each network including VPN
     */
    val networkHandle: Long,
) {


    companion object {
        fun networkStateForActiveNetwork(cm: ConnectivityManager?) = if (cm == null) {
            null
        } else {
            try {
                val activeNetwork = cm.activeNetwork
                val networkCapability = cm.getNetworkCapabilities(activeNetwork)
                val hasInternetCapability = networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
                val hasNotRestrictedInternetCapability = networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED) ?: false
                val isValidated = networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false
                val canConnectToTheInternet = hasInternetCapability && hasNotRestrictedInternetCapability && isValidated
                val isUnMetered = networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ?: true
                val isNotRoaming = if (ApiLevel.isPiePlus()) {
                    networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING) ?: true
                } else {
                    true
                }
                val networkHandle = activeNetwork?.networkHandle ?: 0L

                NetworkState(
                    isDeviceOnline = canConnectToTheInternet,
                    isMetered = !isUnMetered,
                    isRoaming = !isNotRoaming,
                    networkHandle = networkHandle
                )
            } catch (e: Exception) {
                CLog.logPrintStackTrace(e)
                null
            }
        }
    }
}