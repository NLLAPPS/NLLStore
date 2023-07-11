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
import com.nll.store.update.PeriodicUpdateCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.acra.ReportField
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.config.notification
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import java.util.concurrent.Executors


class App : Application(), ImageLoaderFactory {
    companion object {
        private const val logTag = "App"
        lateinit var INSTANCE: App private set
        val applicationScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.Main + SupervisorJob()) }
        val contactEmail = "cb@nllapps.com"
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
        PeriodicUpdateCheckWorker.enqueueUpdateCheck(this)
    }


    private fun initACRA() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "initACRA()")
        }
        try {
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
                    mailTo = contactEmail
                    reportAsFile = false
                }

                notification {
                    title = getString(R.string.crash_notif_title)
                    text = getString(R.string.crash_dialog_text)
                    channelName = getString(R.string.app_crash_notification_channel)
                    sendButtonText = getString(R.string.send)
                    discardButtonText = getString(R.string.cancel)
                    sendOnClick = true
                    resDiscardButtonIcon = R.drawable.crash_log_discard
                    resSendButtonIcon = R.drawable.crash_log_send
                }

                //Notification may not work, also use dialog for now. See https://github.com/ACRA/acra/issues/1146
                dialog {
                    title = getString(R.string.crash_notif_title)
                    text = getString(R.string.crash_dialog_text)

                }
            }


        } catch (e: Exception) {
            //Already called. Ignore. It seems to be called more than once on rare occasions
            CLog.logPrintStackTrace(e)
        }
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