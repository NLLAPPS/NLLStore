package com.nll.store.log

import android.content.Context
import com.nll.store.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

object CLog {
    private const val logTag = "AppLog"

    private val loggerDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT)
    private val loggerScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.Main + SupervisorJob()) }
    private var _debug = BuildConfig.DEBUG
    private val _observableLog = MutableSharedFlow<String>()
    fun observableLog() = _observableLog.asSharedFlow()

    @JvmStatic
    fun isDebug() = _debug
    fun disableDebug() {
        _debug = false
    }

    @JvmStatic
    fun enableDebug(context: Context? = null) {
        _debug = true
    }

    @JvmStatic
    fun logPrintStackTrace(e: Throwable) {
        //We do not want to print stack trace in to log if it is debug build. This would create douple printing of stack traces to logcat
        val shouldLog = isDebug() && !BuildConfig.DEBUG
        if (shouldLog) {
            log(logTag, e.stackTraceToString())
        }
        e.printStackTrace()
    }

    @JvmStatic
    fun log(extraTag: String, message: String) {
        android.util.Log.d("STORE_$extraTag", message)
        loggerScope.launch {
            _observableLog.emit("[${loggerDateFormat.format(System.currentTimeMillis())}] [STORE_$extraTag] => $message")
        }

    }

    @JvmStatic
    fun logAsInfo(extraTag: String, message: String) {
        android.util.Log.i("STORE_$extraTag", message)
        loggerScope.launch {
            _observableLog.emit("[${loggerDateFormat.format(System.currentTimeMillis())}] [STORE_$extraTag] => $message")
        }

    }


}