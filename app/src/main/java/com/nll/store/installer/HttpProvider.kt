package com.nll.store.installer

import com.nll.store.BuildConfig
import com.nll.store.log.CLog
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object HttpProvider {
    private const val logTag = "HttpProvider"
    private const val connectionTimeoutMs: Long = 10000
    private const val readTimeoutMs: Long = 50000
    private const val writeTimeoutMs: Long = 50000
    private val client: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            if (CLog.isDebug()) {
                CLog.log(logTag, message)
            }
        }.apply {
            val logLevel = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            setLevel(logLevel)
            //Security
            //redactHeader("Authorization");
            //redactHeader("Cookie");
        }

        OkHttpClient().newBuilder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(connectionTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS).build()
    }


    fun provideOkHttpClient(): OkHttpClient {
        return client
    }

    fun provideRequestForOwnServer(url: String): Request.Builder {
        return Request.Builder()
            .header("User-Agent", "NLLStore")
            .header("Accept", "*/*")
            .url(url)


    }

    fun provideRequest(url: String): Request.Builder {
        return Request.Builder()
            .header("User-Agent", "NLLStore")
            .header("Accept", "*/*")
            .url(url)

    }
}