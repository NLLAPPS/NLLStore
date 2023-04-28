package com.nll.store

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.nll.store.connectivity.InternetStateProvider
import com.nll.store.log.CLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.Executors


class App : Application(), ImageLoaderFactory {
    companion object {
        private const val logTag = "App"
        lateinit var INSTANCE: App private set
        val applicationScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.Main + SupervisorJob()) }

    }


    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        //Use executor rather than coroutine https://medium.com/specto/android-startup-tip-dont-use-kotlin-coroutines-a7b3f7176fe5
        //However, he was wrong. https://medium.com/specto/dont-run-benchmarks-on-a-debuggable-android-app-like-i-did-34d95331cabb
        //Keep for now
        Executors.newSingleThreadExecutor().execute {
            initACRA()
        }

    }

    override fun onCreate() {
        super.onCreate()
        if (CLog.isDebug()) {
            CLog.log(logTag, "onCreate()")
        }
        INSTANCE = this
        initACRA()
        InternetStateProvider.start(this)
    }



    private fun initACRA() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "initACRA()")
        }
        /* try {
             initAcra {

                 buildConfigClass = BuildConfig::class.java
                 reportFormat = StringFormat.KEY_VALUE_LIST
                 reportContent = listOf(
                     ReportField.USER_COMMENT,
                     ReportField.PACKAGE_NAME,
                     ReportField.APP_VERSION_NAME,
                     ReportField.ANDROID_VERSION,
                     ReportField.BRAND,
                     ReportField.PHONE_MODEL,
                     ReportField.PRODUCT,
                     ReportField.USER_APP_START_DATE,
                     ReportField.USER_CRASH_DATE,
                     ReportField.STACK_TRACE,
                     ReportField.LOGCAT
                 )

                 mailSender {
                     mailTo = StoreConfigImpl.getStoreContactEmail()
                     reportAsFile = false
                 }

                 notification {
                     title = getString(AppResources.string.crash_notif_title)
                     text = getString(AppResources.string.crash_dialog_text)
                     channelName = getString(AppResources.string.app_crash_notification_channel)
                     sendButtonText = getString(AppResources.string.send)
                     discardButtonText = getString(AppResources.string.cancel)
                     sendOnClick = true
                     resDiscardButtonIcon = AppResources.drawable.crash_log_discard
                     resSendButtonIcon = AppResources.drawable.crash_log_send
                 }
             }


         } catch (e: Exception) {
             //Already called. Ignore. It seems to be called more than once on rare occasions
             CLog.logPrintStackTrace(e)
         }*/
    }

    override fun newImageLoader() = ImageLoader.Builder(this)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .logger(DebugLogger())
        .respectCacheHeaders(false)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(5 * 1024 * 1024)
                .build()
        }
        .build()
}