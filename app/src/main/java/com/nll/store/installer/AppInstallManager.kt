package com.nll.store.installer

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import com.nll.store.log.CLog
import com.nll.store.model.LocalAppData
import com.nll.store.model.StoreAppData
import com.nll.store.ui.extCopyTo
import com.nll.store.utils.ApiLevel
import com.nll.store.utils.CoroutineScopeFactory
import com.nll.store.utils.getPackageInfoFromApk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * TODO explore https://developer.android.com/about/versions/14/features#app-stores
 * Also follow developments at https://gitlab.com/fdroid/fdroidclient/-/issues/1836
 */
object AppInstallManager {
    private const val logTag = "AppInstallManager"

    private val iOScope by lazy { CoroutineScopeFactory.create(Dispatchers.IO) }
    private var installJob: Job? = null
    private val _installationState = MutableSharedFlow<InstallationState>()
    var hasActiveSession = false
        private set

    fun cancelInstall() {
        installJob?.cancel()
    }
    fun observeInstallState() = _installationState.asSharedFlow()
    fun updateInstallState(installationState: InstallationState) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "updateInstallState() -> installationState: $installationState")
        }
        iOScope.launch {
            _installationState.emit(installationState)
        }

    }


    fun startDownload(context: Context, storeAppData: StoreAppData, localAppData: LocalAppData?) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "startDownload() -> storeAppData: $storeAppData, localAppData: $localAppData")
        }
        iOScope.launch {
            val targetFile = FileDownloader.getDestinationFile(context.applicationContext, storeAppData)

            val shouldDownload = if (targetFile.exists()) {

                /**
                 * While we check packageInfo at setProgress when state is Completed
                 * We still need to check here too.
                 * Imagine a previously downloaded file being invalid and new version info being published.
                 * We must make sure we ignore invalid files
                 */
                val downloadedApk = targetFile.getPackageInfoFromApk(context.applicationContext)
                if (downloadedApk != null) {
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "startDownload() -> downloadedApk.longVersionCode: ${downloadedApk.longVersionCode}, storeAppData.version: ${localAppData?.versionCode}")
                    }
                    if (localAppData == null) {
                        if (CLog.isDebug()) {
                            CLog.log(logTag, "startDownload() -> We have a downloaded file and no installed version. No need to download")
                        }
                        updateInstallState( InstallationState.Download.Completed(storeAppData, targetFile, downloadedApk))
                        false
                    } else {

                        //Check if downloaded apk's version is equals or higher than current installed version.
                        val isNewOrSameVersion = downloadedApk.longVersionCode >= localAppData.versionCode
                        //Check if downloaded apk's version is smaller than store version
                        val forceDownload = downloadedApk.longVersionCode < storeAppData.version

                        if (CLog.isDebug()) {
                            CLog.log(logTag, "startDownload() -> isNewOrSameVersion: $isNewOrSameVersion, forceDownload: $forceDownload")
                        }
                        if (isNewOrSameVersion) {
                            if (forceDownload) {
                                if (CLog.isDebug()) {
                                    CLog.log(logTag, "startDownload() -> We have a downloaded file with same or new version of installed version but its version is smaller than store version. Force downloading")
                                }
                                true
                            } else {
                                if (CLog.isDebug()) {
                                    CLog.log(logTag, "startDownload() -> We have a downloaded file with same or new version of installed version. No need to download. Updating state to Completed")
                                }
                                updateInstallState( InstallationState.Download.Completed(storeAppData, targetFile, downloadedApk))
                                false
                            }
                        } else {
                            if (CLog.isDebug()) {
                                CLog.log(logTag, "startDownload() -> We have a downloaded file but it is NOT same or new version as requested. Allow download")
                            }
                            true
                        }
                    }

                } else {
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "startDownload() -> We have a download file but it is corrupt. Allow download")
                    }
                    true
                }
            } else {
                if (CLog.isDebug()) {
                    CLog.log(logTag, "startDownload() -> We have no file. Allow download")
                }
                true
            }

            if (CLog.isDebug()) {
                CLog.log(logTag, "startDownload() -> shouldDownload: $shouldDownload, targetFile: ${targetFile.absolutePath}")
            }

            if (shouldDownload) {

                FileDownloader().download(context.applicationContext, storeAppData, targetFile, object : FileDownloader.Callback {

                    override fun onStarted(storeAppData: StoreAppData) {
                        updateInstallState( InstallationState.Download.Started(storeAppData))
                    }

                    override fun onProgress(storeAppData: StoreAppData, percent: Int, bytesCopied: Int, length: Long) {
                        updateInstallState( InstallationState.Download.Progress(storeAppData, percent, bytesCopied, length))
                    }

                    override fun onCompleted(storeAppData: StoreAppData, targetFile: File, packageInfo: PackageInfo) {
                        updateInstallState( InstallationState.Download.Completed(storeAppData, targetFile, packageInfo))
                    }

                    override fun onError(storeAppData: StoreAppData, exception: Exception) {
                        updateInstallState( InstallationState.Download.Error(storeAppData, InstallationState.Download.Error.Message.GenericError(exception.message ?: "NULL")))
                    }

                    override fun onServerError(storeAppData: StoreAppData, responseCode: Int) {
                        updateInstallState( InstallationState.Download.Error(storeAppData, InstallationState.Download.Error.Message.ServerError(responseCode)))
                    }

                    override fun onMalformedFileError(storeAppData: StoreAppData) {
                        updateInstallState( InstallationState.Download.Error(storeAppData, InstallationState.Download.Error.Message.MalformedFile))
                    }

                })

            }
        }

    }


    fun install(context: Context, packageName: String?, apkFiles: Array<ApkSource>) {

        if (!hasActiveSession) {
            hasActiveSession = true

            iOScope.launch {
                updateInstallState( InstallationState.Install.Started)
                var session: PackageInstaller.Session? = null
                try {
                    val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                        setInstallReason(PackageManager.INSTALL_REASON_USER)

                        packageName?.let {
                            setAppPackageName(it)
                        }
                         setOriginatingUid(android.os.Process.myUid())


                        /**
                         * Allow auto update
                         * https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams#setRequireUserAction(int)
                         */
                        if (ApiLevel.isSPlus()) {
                            setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                        }

                        if(ApiLevel.isTPlus()){
                            setPackageSource(PackageInstaller.PACKAGE_SOURCE_STORE)
                        }

                    }

                    val sessionId = context.packageManager.packageInstaller.createSession(sessionParams)
                    withContext(Dispatchers.Main) {
                        context.packageManager.packageInstaller.registerSessionCallback(packageInstallerSessionCallback(sessionId))
                    }
                    session = context.packageManager.packageInstaller.openSession(sessionId)
                    session.copyApksFrom(context, apkFiles)
                    session.commit(InstallationEventsReceiver.createIntentSender(context))
                } catch (e: IOException) {
                    hasActiveSession = false
                    CLog.logPrintStackTrace(e)
                    updateInstallState( InstallationState.Install.Completed(PackageInstallResult.Failure(PackageInstallFailureCause.Generic(e.message))))
                } catch (e: RuntimeException) {
                    hasActiveSession = false
                    session?.abandon()
                    updateInstallState( InstallationState.Install.Completed(PackageInstallResult.Failure(PackageInstallFailureCause.Aborted(e.message))))
                    CLog.logPrintStackTrace(e)
                }
            }
        } else {
            updateInstallState( InstallationState.Install.Completed(PackageInstallResult.Failure(PackageInstallFailureCause.Aborted("Can't install while another install session is active."))))
        }
    }

    private fun PackageInstaller.Session.copyApksFrom(context: Context, apkFiles: Array<ApkSource>) {
        val totalLength = apkFiles.sumOf { it.getLength(context) }
        var transferredBytes = 0L
        apkFiles.forEachIndexed { index, apkFile ->
            val apkFileLength = apkFile.getLength(context)
            // though `copyTo` closes streams, we need to ensure that if opening sessionStream fails, apkStream is closed
            apkFile.getInputStream(context).use { apkStream ->
                val sessionStream = openWrite("temp$index.apk", 0, apkFileLength)
                apkStream.extCopyTo(sessionStream, totalLength, transferredBytes, onProgressChanged = { progress, max ->
                    setStagingProgress(progress.toFloat() / max)
                })
            }
            transferredBytes += apkFileLength
        }
    }


    private fun packageInstallerSessionCallback(currentSessionId: Int) = object : PackageInstaller.SessionCallback() {
        override fun onCreated(sessionId: Int) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "packageInstallerSessionCallback() -> onCreated() -> sessionId: $sessionId, currentSessionId: $currentSessionId")
            }
            hasActiveSession = true
        }

        override fun onBadgingChanged(sessionId: Int) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "packageInstallerSessionCallback() -> onBadgingChanged() -> sessionId: $sessionId, currentSessionId: $currentSessionId")
            }
        }

        override fun onActiveChanged(sessionId: Int, active: Boolean) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "packageInstallerSessionCallback() -> onActiveChanged() -> sessionId: $sessionId, active: $active, currentSessionId: $currentSessionId")
            }
        }

        override fun onFinished(sessionId: Int, success: Boolean) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "packageInstallerSessionCallback() -> onFinished() -> sessionId: $sessionId, success: $success, currentSessionId: $currentSessionId")
            }
            hasActiveSession = false
            /**
             * InstallationEventsReceiver provides better results with error messages. Using that for now.
             */
            /*if (sessionId == currentSessionId) {
                if (success) {
                    updateInstallState( InstallationState.Install.Completed(PackageInstallResult.Success)
                } else {
                    updateInstallState( InstallationState.Install.Completed(PackageInstallResult.Failure(PackageInstallFailureCause.Generic("Error onFinished()")))
                }
            }*/

        }

        override fun onProgressChanged(sessionId: Int, progress: Float) {
            if (sessionId == currentSessionId) {
                updateInstallState( InstallationState.Install.Progress(InstallationState.Install.ProgressData((progress * 100).toInt(), 100)))
            }
        }
    }

}
