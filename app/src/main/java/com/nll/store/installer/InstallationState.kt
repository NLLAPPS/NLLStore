package com.nll.store.installer

import android.content.Context
import android.content.pm.PackageInfo
import com.nll.store.ApkAttachmentProvider
import com.nll.store.model.StoreAppData
import java.io.File

sealed class InstallationState {
    sealed class Download(open val storeAppData: StoreAppData) : InstallationState() {
        data class Started(override val storeAppData: StoreAppData) : Download(storeAppData)
        data class Progress(override val storeAppData: StoreAppData, val percent: Int, val bytesCopied: Int, val totalBytes: Long) : Download(storeAppData)
        data class Completed(override val storeAppData: StoreAppData, val downloadedFile: File, val packageInfo: PackageInfo) : Download(storeAppData) {
            fun getContentUri(context: Context) = ApkAttachmentProvider.getUri(context, downloadedFile.name)
        }

        data class Error(override val storeAppData: StoreAppData, val message: Message) : Download(storeAppData) {

            sealed class Message {
                data class GenericError(val message: String) : Message()
                data class ServerError(val responseCode: Int) : Message()
                data object MalformedFile : Message()
            }
        }
    }

    sealed class Install : InstallationState() {
        data class ProgressData(
            val progress: Int = 0,
            val max: Int = 100,
            val isIndeterminate: Boolean = false
        )

        data object Started : Install()
        data class Progress(val progress: ProgressData) : Install()
        data class Completed(val installResult: PackageInstallResult) : Install()


    }
}