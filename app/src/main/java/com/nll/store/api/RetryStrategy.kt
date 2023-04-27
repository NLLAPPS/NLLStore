package com.nll.store.api

import  kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

 data class RetryStrategy(
     val maxRetries: Int = 3,
     val base: Double = 2.0,
     val maxDelay: Duration = 60.seconds,
)