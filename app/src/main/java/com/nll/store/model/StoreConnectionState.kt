package com.nll.store.model

import com.nll.store.api.ApiException

sealed class StoreConnectionState {
    data object Connecting : StoreConnectionState()
    data object Connected : StoreConnectionState()
    data class Failed(val apiException: ApiException) : StoreConnectionState()
}