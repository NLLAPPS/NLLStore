package com.nll.store.model

import com.nll.store.api.ApiException

sealed class StoreConnectionState {
    object Connecting : StoreConnectionState()
    object Connected : StoreConnectionState()

    class Failed(val apiException: ApiException) : StoreConnectionState()
}