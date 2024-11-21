package com.nll.store.utils

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * Kotlin's native debounce() and sample() does not meet our needs.
 * We need a debounce where it always emits the latest value after set millis seconds
 * debounce() -> Does not emit anything as long as the original flow emits items faster than every timeoutMillis milliseconds. So we do not get the latest value if network changes faster than milliseconds we use
 * sample() -> The latest element is not emitted if it does not fit into the sampling window. So we do not get the latest value if network changes faster than milliseconds we use
 */
fun <T> Flow<T>.extDebounce(waitMillis: Long) = channelFlow {
    /*if (CLog.isDebug()) {
        CLog.log(logTag, "extDebounce() -> Thread is ${Thread.currentThread()}")
    }*/

    launch(Dispatchers.IO) {

        /*if (CLog.isDebug()) {
            CLog.log(logTag, "extDebounce() -> launch -> Thread is ${Thread.currentThread()}")
        }*/

        var delayPost: Deferred<Unit>? = null

        collect {

            /* if (CLog.isDebug()) {
                CLog.log(logTag, "extDebounce() -> collect: $it -> Thread is ${Thread.currentThread()}")
            }*/

            delayPost?.cancel()
            delayPost = async {

                /*if (CLog.isDebug()) {
                    CLog.log(logTag, "extDebounce() -> async() -> Thread is ${Thread.currentThread()}")
                }*/

                delay(waitMillis)

                /*if (CLog.isDebug()) {
                    CLog.log(logTag, "extDebounce() -> send($it) -> Thread is ${Thread.currentThread()}")
                }*/

                send(it)
            }
        }
    }
}