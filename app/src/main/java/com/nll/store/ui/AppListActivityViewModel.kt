package com.nll.store.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nll.store.R
import com.nll.store.api.NLLStoreApi
import com.nll.store.log.CLog
import com.nll.store.model.AppData
import com.nll.store.model.InstallState
import com.nll.store.model.LocalAppData
import com.nll.store.utils.getInstalledApplicationsCompat
import io.github.solrudev.simpleinstaller.PackageInstaller
import io.github.solrudev.simpleinstaller.PackageUninstaller
import io.github.solrudev.simpleinstaller.data.ConfirmationStrategy
import io.github.solrudev.simpleinstaller.data.InstallFailureCause
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.data.notification
import io.github.solrudev.simpleinstaller.installPackage
import io.github.solrudev.simpleinstaller.uninstallPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AppListActivityViewModel(private val app: Application) : AndroidViewModel(app) {
    private val logTag = "AppListActivityViewModel"
    private val nllStoreApi = NLLStoreApi()
    private val _appsList = MutableStateFlow(listOf<AppData>())
    val appsList = _appsList.asStateFlow()
    private val _installState = MutableStateFlow<InstallState>(InstallState.Idle)
    val installState = _installState.asStateFlow()
    private var installJob: Job? = null
    private var lastAppListLoadTime = 0L
    val isUninstalling get() = PackageUninstaller.hasActiveSession
    val isInstalling get() = PackageInstaller.hasActiveSession

    fun loadAppList() {

        val shouldLoad = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - lastAppListLoadTime) > 10
        if (CLog.isDebug()) {
            CLog.log(logTag, "loadAppList() -> shouldLoad: $shouldLoad")
        }

        if(shouldLoad) {
            viewModelScope.launch(Dispatchers.IO) {
                val localAppList = getInstalledAppsList("com.nll")
                if (CLog.isDebug()) {
                    CLog.log(logTag, "loadAppList() -> localAppList: ${localAppList.joinToString("\n")}")
                }
                val storeAppList = nllStoreApi.getStoreAppList()
                if (CLog.isDebug()) {
                    CLog.log(logTag, "loadAppList() -> storeAppList: ${storeAppList.joinToString("\n")}")
                }

                _appsList.value = storeAppList.map { storeAppData ->
                    AppData(storeAppData, localAppList.firstOrNull { it.packageName == storeAppData.packageName })
                }
                lastAppListLoadTime = System.currentTimeMillis()
            }
        }
    }

    private fun getInstalledAppsList(packageNameToFilter: String) = app.packageManager.getInstalledApplicationsCompat(0)
        //Exclude ourselves
        .filter { it.packageName != app.packageName }
        .filter { it.packageName.startsWith(packageNameToFilter) }
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

    fun uninstallPackage(appData: AppData) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "uninstallPackage() -> appData: $appData")
        }

        appData.localAppData?.let { localAppData ->
            _installState.value = InstallState.Uninstalling(localAppData)
            viewModelScope.launch {
                val result = PackageUninstaller.uninstallPackage(localAppData.packageName) {
                    confirmationStrategy = ConfirmationStrategy.IMMEDIATE
                }
                if (result) {
                    loadAppList()
                }
            }
        } ?: throw IllegalArgumentException("Cannot uninstall null localAppData!")

    }

    fun installPackage(uri: Uri) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "installPackage() -> uri: $uri")
        }
        _installState.value = InstallState.Installing(uri)
        installJob = viewModelScope.launch(Dispatchers.IO) {
            try {

                val installResult = PackageInstaller.installPackage(uri) {
                    setConfirmationStrategy(ConfirmationStrategy.IMMEDIATE)
                    notification {
                        icon = R.drawable.ic_install
                    }
                }
                if (CLog.isDebug()) {
                    CLog.log(logTag, "installPackage() -> installResult: $installResult")
                }
                _installState.value = InstallState.Completed(uri, installResult)

            } catch (e: Exception) {
                _installState.value = InstallState.Completed(uri, InstallResult.Failure(InstallFailureCause.Aborted(e.message)))
            }
        }
    }

    fun cancelInstall() {
        installJob?.cancel()
    }


    @Suppress("UNCHECKED_CAST")
    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppListActivityViewModel(app) as T
        }
    }
}