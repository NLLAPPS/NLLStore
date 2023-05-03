package com.nll.store.installer

import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import com.nll.store.ApkAttachmentProvider
import com.nll.store.R
import com.nll.store.log.CLog
import com.nll.store.model.LocalAppData
import com.nll.store.model.StoreAppData
import com.nll.store.utils.CoroutineScopeFactory
import com.nll.store.utils.SingletonHolder
import com.nll.store.utils.getPackageInfoFromApk
import io.github.solrudev.simpleinstaller.PackageInstaller
import io.github.solrudev.simpleinstaller.apksource.FileApkSource
import io.github.solrudev.simpleinstaller.apksource.UriApkSource
import io.github.solrudev.simpleinstaller.data.ConfirmationStrategy
import io.github.solrudev.simpleinstaller.data.InstallFailureCause
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.data.ProgressData
import io.github.solrudev.simpleinstaller.data.SessionOptions
import io.github.solrudev.simpleinstaller.data.notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AppInstallManager private constructor(private val applicationContext: Context) {
    private val logTag = "InstallManager"
    private val iOScope by lazy { CoroutineScopeFactory.create(Dispatchers.IO) }
    private val packageInstaller = PackageInstaller.getInstance()
    private var installJob: Job? = null
    private val _installState = MutableStateFlow<State?>(null)
    private val packageInstallerCallback = object : PackageInstaller.Callback {
        override fun onCanceled() {
            if (CLog.isDebug()) {
                CLog.log(logTag, "packageInstallerCallback -> onCanceled()")
            }
            _installState.value = State.Install.Completed(InstallResult.Failure(InstallFailureCause.Generic("Cancelled")))
        }

        override fun onException(exception: Throwable) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "packageInstallerCallback -> onException() -> exception: $exception")
            }
            _installState.value = State.Install.Completed(InstallResult.Failure(InstallFailureCause.Aborted(exception.message)))
        }

        override fun onFailure(cause: InstallFailureCause?) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "packageInstallerCallback -> onFailure() -> cause: $cause")
            }
            _installState.value = State.Install.Completed(InstallResult.Failure(cause))
        }

        override fun onProgressChanged(progress: ProgressData) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "packageInstallerCallback -> onProgressChanged() -> progress: $progress")
            }
            _installState.value = State.Install.Progress(progress)
        }

        override fun onSuccess() {
            if (CLog.isDebug()) {
                CLog.log(logTag, "packageInstallerCallback -> onSuccess() ")
            }
            _installState.value = State.Install.Completed(InstallResult.Success)
        }

    }
    companion object : SingletonHolder<AppInstallManager, Context>({
        AppInstallManager(it.applicationContext)
    })

    fun observeInstallState() = _installState.asStateFlow()
    fun isInstalling() = packageInstaller.hasActiveSession
    fun cancelInstall() {
        installJob?.cancel()
    }


    private fun getSessionOptions() = SessionOptions {
        //TODO pass setWhitelistedRestrictedPermissions() as sessionoption?
        setConfirmationStrategy(ConfirmationStrategy.IMMEDIATE)
        notification {
            icon = R.drawable.ic_install
        }
    }

    fun install(uri: Uri) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "install() -> uri: $uri")
        }
        installJob = iOScope.launch {
            packageInstaller.installPackage(UriApkSource(uri), getSessionOptions(), packageInstallerCallback)

        }
    }

    fun install(file: File) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "install() -> file: $file")
        }

        installJob = iOScope.launch {
            packageInstaller.installPackage(FileApkSource(file), getSessionOptions(), packageInstallerCallback)

        }
    }

    fun startDownload(storeAppData: StoreAppData, localAppData: LocalAppData?) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "startDownload() -> storeAppData: $storeAppData, localAppData: $localAppData")
        }
        iOScope.launch {
            val targetFile = FileDownloader.getDestinationFile(applicationContext, storeAppData)

            val shouldDownload = if (targetFile.exists()) {

                /**
                 * While we check packageInfo at setProgress when state is Completed
                 * We still need to check here too.
                 * Imagine a previously downloaded file being invalid and new version info being published.
                 * We must make sure we ignore invalid files
                 */
                val downloadedApk = targetFile.getPackageInfoFromApk(applicationContext)
                if (downloadedApk != null) {
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "startDownload() -> downloadedApk.longVersionCode: ${downloadedApk.longVersionCode}, storeAppData.version: ${localAppData?.versionCode}")
                    }
                    if (localAppData == null) {
                        if (CLog.isDebug()) {
                            CLog.log(logTag, "startDownload() -> We have a downloaded file and no installed version. No need to download")
                        }
                        _installState.value = State.Download.Completed(storeAppData, targetFile, downloadedApk)
                        false
                    } else {

                        //Check if downloaded apk's version is equals or higher than current installed version.
                        val isNewOrSameVersion = downloadedApk.longVersionCode >= localAppData.versionCode
                        //Check if downloaded apk's version is smaller than store version
                        val forceDownload = downloadedApk.longVersionCode<storeAppData.version

                        if (CLog.isDebug()) {
                            CLog.log(logTag, "startDownload() -> isNewOrSameVersion: $isNewOrSameVersion, forceDownload: $forceDownload")
                        }
                        if (isNewOrSameVersion) {
                            if(forceDownload){
                                if (CLog.isDebug()) {
                                    CLog.log(logTag, "startDownload() -> We have a downloaded file with same or new version of installed version but its version is smaller than store version. Force downloading")
                                }
                               true
                            }else {
                                if (CLog.isDebug()) {
                                    CLog.log(logTag, "startDownload() -> We have a downloaded file with same or new version of installed version. No need to download. Updating state to Completed")
                                }
                                _installState.value = State.Download.Completed(storeAppData, targetFile, downloadedApk)
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

                FileDownloader().download(applicationContext, storeAppData, targetFile) { downloadState ->
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "startDownload() -> downloadState: $downloadState")
                    }
                    _installState.value = downloadState
                }

            }
        }

    }

    sealed class State {
        sealed class Download(open val storeAppData: StoreAppData) : State() {
            data class Started(override val storeAppData: StoreAppData) : Download(storeAppData)
            data class Progress(override val storeAppData: StoreAppData, val percent: Int, val bytesCopied: Int, val totalBytes: Long) : Download(storeAppData)
            data class Completed(override val storeAppData: StoreAppData, val downloadedFile: File, val packageInfo: PackageInfo) : Download(storeAppData) {

                fun getContentUri(context: Context) = ApkAttachmentProvider.getUri(context, downloadedFile.name)
            }

            data class Error(override val storeAppData: StoreAppData, val message: Message) : Download(storeAppData) {

                sealed class Message {
                    data class GenericError(val message: String) : Message()
                    data class ServerError(val responseCode: Int) : Message()
                    object MalformedFile : Message()

                }
            }
        }

        sealed class Install : State() {
            object Started : Install()
            data class Progress(val progress: ProgressData) : Install()
            data class Completed(val installResult: InstallResult) : Install()


        }
    }
}