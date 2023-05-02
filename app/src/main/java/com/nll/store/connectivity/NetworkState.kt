package com.nll.store.connectivity


data class NetworkState(

    private val hasInternetCapability: Boolean,
    /**
     * Determines if the network is metered. Such as Mobile Data
     * It also allows us to inform observers that actual data connection type changed (from wifi to mobile and vice versa) which is useful on SIP connections
     */
    private val isMetered: Boolean,

    private val isRoaming: Boolean,

    /**
     * To make sure we broadcast changes even if hasInternetCapability/isMetered not changed. Perhaps user has 2 sim cards or connected to vpn. networkHandle changes for each network including VPN
     */
    private val networkHandle: Long
) {
    fun isConnectionUnMetered() = isMetered.not()
    fun isDeviceOnline() = hasInternetCapability
    fun isRoaming() = isRoaming
}