package com.nll.store.ui

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nll.store.api.ApiException
import com.nll.store.api.NLLStoreApi
import com.nll.store.log.CLog
import com.nll.store.model.AppData
import com.nll.store.model.AppInstallState
import com.nll.store.model.LocalAppData
import com.nll.store.model.StoreAppData
import com.nll.store.model.StoreConnectionState
import com.nll.store.utils.PackageReceiver
import com.nll.store.utils.getInstalledApplicationsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class StoreApiViewModel(private val app: Application) : AndroidViewModel(app) {
    private val logTag = "StoreApiViewModel"
    private val nllPackages = "com.nll."
    private val nllStoreApi = NLLStoreApi()
    private val _appsList = MutableStateFlow(listOf<AppData>())
    val appsList = _appsList.asStateFlow()
    private val _storeConnectionState = MutableStateFlow<StoreConnectionState>(StoreConnectionState.Connected)
    val storeConnectionState = _storeConnectionState.asStateFlow()
    private var lastStoreAppListLoadTime = 0L
    private var storeAppList: List<StoreAppData> = listOf()


    init {
        registerPackageReceiver()
    }

    fun loadAppList() {

        //Rate limit remote connectivity
        val shouldLoadFromRemote = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - lastStoreAppListLoadTime) > 60
        if (CLog.isDebug()) {
            CLog.log(logTag, "loadAppList() -> shouldLoadFromRemote: $shouldLoadFromRemote")
        }

        _storeConnectionState.value = StoreConnectionState.Connecting

        viewModelScope.launch(Dispatchers.IO) {
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


    private fun getInstalledAppsList() = app.packageManager.getInstalledApplicationsCompat(0)
        //Exclude ourselves
        .filter { it.packageName != app.packageName }
        .filter { it.packageName.startsWith(nllPackages) }
        .map {
            val icon = app.packageManager.getApplicationIcon(it)
            val appName = app.packageManager.getApplicationLabel(it) as String
            val packageName = it.packageName
            /*
               Be careful when replacing deprecated method below with something like PackageManager.PackageInfoFlags.of(PackageManager.GET_INSTRUMENTATION)
               getPackageInfo(packageName, 0) returns all info. We need to make sure new method would return all the info we use
               for example, we need firstInstallTime. Which PackageManager.GET_.. returns it?
            */
            val versionCode = app.packageManager.getPackageInfo(packageName, 0).longVersionCode
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

        app.registerReceiver(PackageReceiver { packageName ->
            if (CLog.isDebug()) {
                CLog.log(logTag, "registerPackageReceiver() -> callback() -> packageName: $packageName")
            }
            loadAppList()
        }, pkgFilter)
    }


    @Suppress("UNCHECKED_CAST")
    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StoreApiViewModel(app) as T
        }
    }
}