package com.nll.store.api
import kotlin.time.Duration

/**
 * Http operations timeouts.
 *
 * @param request time period required to process an HTTP call: from sending a request to receiving a response
 * @param connect time period in which a client should establish a connection with a server
 * @param socket maximum time of inactivity between two data packets when exchanging data with a server
 */
data class Timeout(
     val request: Duration,
     val connect: Duration,
     val socket: Duration,
)