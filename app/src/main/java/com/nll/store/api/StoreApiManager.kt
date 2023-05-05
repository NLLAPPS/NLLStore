package com.nll.store.api

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.nll.store.log.CLog
import com.nll.store.model.AppData
import com.nll.store.model.AppInstallState
import com.nll.store.model.LocalAppData
import com.nll.store.model.StoreAppData
import com.nll.store.model.StoreConnectionState
import com.nll.store.update.UpdateNotification
import com.nll.store.utils.ApiLevel
import com.nll.store.utils.CoroutineScopeFactory
import com.nll.store.utils.SingletonHolder
import com.nll.store.utils.getInstalledApplicationsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class StoreApiManager private constructor(private val applicationContext: Context) {
    private val logTag = "StoreApiManager"
    private val iOScope by lazy { CoroutineScopeFactory.create(Dispatchers.IO) }
    private val nllPackages = "com.nll."
    private val nllStoreApi = NLLStoreApi()
    private val _appsList = MutableStateFlow(listOf<AppData>())
    private val _storeConnectionState = MutableStateFlow<StoreConnectionState>(StoreConnectionState.Connected)
    private var lastStoreAppListLoadTime = 0L
    private var storeAppList: List<StoreAppData> = listOf()

    companion object : SingletonHolder<StoreApiManager, Context>({
        StoreApiManager(it.applicationContext)
    })

    init {
        registerPackageReceiver()
    }


    fun observeAppList() = _appsList.asStateFlow()
    fun observeStoreConnectionState() = _storeConnectionState.asStateFlow()
    fun loadAppList() {

        //Rate limit remote connectivity
        val shouldLoadFromRemote = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - lastStoreAppListLoadTime) > 60
        if (CLog.isDebug()) {
            CLog.log(logTag, "loadAppList() -> shouldLoadFromRemote: $shouldLoadFromRemote")
        }

        _storeConnectionState.value = StoreConnectionState.Connecting

        iOScope.launch {
            val localAppList = try {
                getInstalledAppsList()
            } catch (e: Exception) {
                if (CLog.isDebug()) {
                    CLog.log(logTag, "loadAppList() -> Error while loading localAppList")
                }
                listOf()
            }
            if (CLog.isDebug()) {
                CLog.log(logTag, "loadAppList() -> localAppList: ${localAppList.joinToString("\n")}")
            }
            try {
                storeAppList = if (shouldLoadFromRemote) {
                    nllStoreApi.getStoreAppList()
                } else {
                    storeAppList
                }
                if (CLog.isDebug()) {
                    CLog.log(logTag, "loadAppList() -> storeAppList: ${storeAppList.joinToString("\n")}")
                }
                _appsList.value = storeAppList.map { storeAppData ->
                    val localAppData = localAppList.firstOrNull { it.packageName == storeAppData.packageName }
                    val appInstallState = if (localAppData == null) {
                        AppInstallState.NotInstalled
                    } else {
                        AppInstallState.Installed(localAppData)
                    }
                    AppData(storeAppData, appInstallState)
                }
                lastStoreAppListLoadTime = System.currentTimeMillis()
                _storeConnectionState.value = StoreConnectionState.Connected
            } catch (e: Exception) {
                if (CLog.isDebug()) {
                    CLog.log(logTag, "loadAppList() -> Error while requesting app list!")
                }
                CLog.logPrintStackTrace(e)
                _storeConnectionState.value = StoreConnectionState.Failed(e as? ApiException ?: ApiException.GenericException(e))

            }
        }

    }


    private fun getInstalledAppsList() = applicationContext.packageManager.getInstalledApplicationsCompat(0)
        .filter { it.packageName.startsWith(nllPackages) }
        .map {
            val icon = applicationContext.packageManager.getApplicationIcon(it)
            val appName = applicationContext.packageManager.getApplicationLabel(it) as String
            val packageName = it.packageName
            /*
               Be careful when replacing deprecated method below with something like PackageManager.PackageInfoFlags.of(PackageManager.GET_INSTRUMENTATION)
               getPackageInfo(packageName, 0) returns all info. We need to make sure new method would return all the info we use
               for example, we need firstInstallTime. Which PackageManager.GET_.. returns it?
            */
            val versionCode = applicationContext.packageManager.getPackageInfo(packageName, 0).longVersionCode
            LocalAppData(packageName.hashCode(), icon, appName, packageName, versionCode)
        }
        .sortedBy { it.name }

    private fun registerPackageReceiver() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "registerPackageReceiver()")
        }
        val pkgFilter = IntentFilter().apply {
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_DATA_CLEARED)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }

        ContextCompat.registerReceiver(applicationContext, PackageReceiver { packageName ->
            if (CLog.isDebug()) {
                CLog.log(logTag, "registerPackageReceiver() -> callback() -> packageName: $packageName")
            }
            loadAppList()
        }, pkgFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    /**
     * TODO Implement automatic updating if APH
     */
    fun checkUpdates() {
        if (CLog.isDebug()) {
            CLog.log(logTag, "checkUpdates()")
        }

        if (hasNotificationPermission()) {
            //Update app list cache
            loadAppList()
            //Check for updates
            val postUpdateNotification = _appsList.value.any { it.canBeUpdated() }
            if (CLog.isDebug()) {
                CLog.log(logTag, "checkUpdates() -> postUpdateNotification: $postUpdateNotification")
            }
            if(postUpdateNotification){
                UpdateNotification.postUpdateNotification(applicationContext)
            }
        } else {
            if (CLog.isDebug()) {
                CLog.log(logTag, "checkUpdates() -> We do not have notification permission. Skipping update check")
            }
        }

    }

    private fun hasNotificationPermission() = if (ApiLevel.isTPlus()) {
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}