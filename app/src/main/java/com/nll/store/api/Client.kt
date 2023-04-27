package com.nll.store.api

import com.nll.store.log.CLog
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import io.ktor.util.appendIfNameAbsent
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

object Client {
    private const val logTag = "Client"

    private const val apiUrl = "https://acr.app/store/api/"
    private val timeoutConfig = Timeout(request = 30.seconds, connect = 30.seconds, socket = 30.seconds)
    private val retryStrategy = RetryStrategy()
    private val headerParams: Map<String, String> = emptyMap()
    private val queryParams: Map<String, String> = emptyMap()
    internal fun createHttpClient() = HttpClient {

        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                KotlinxSerializationConverter(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            )
        }

        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    if (CLog.isDebug()) {
                        CLog.log(logTag, message)
                    }
                }
            }
        }


        install(HttpTimeout) {
            socketTimeoutMillis = timeoutConfig.socket.toLong(DurationUnit.MILLISECONDS)
            connectTimeoutMillis = timeoutConfig.connect.toLong(DurationUnit.MILLISECONDS)
            requestTimeoutMillis = timeoutConfig.request.toLong(DurationUnit.MILLISECONDS)
        }

        install(HttpRequestRetry) {
            maxRetries = retryStrategy.maxRetries
            // retry on rate limit error.
            retryIf { _, response -> response.status.value.let { it == 429 } }
            exponentialDelay(retryStrategy.base, retryStrategy.maxDelay.inWholeMilliseconds)
        }

        defaultRequest {
            url(apiUrl)
            accept(ContentType.Any)
            queryParams.onEach { (key, value) -> url.parameters.appendIfNameAbsent(key, value) }
            headerParams.onEach { (key, value) -> headers.appendIfNameAbsent(key, value) }
        }

        expectSuccess = true
    }

}
