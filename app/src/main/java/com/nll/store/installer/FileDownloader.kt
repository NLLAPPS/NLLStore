package com.nll.store.installer

import android.content.Context
import android.content.pm.PackageInfo
import android.webkit.MimeTypeMap
import com.nll.store.log.CLog
import com.nll.store.model.StoreAppData
import com.nll.store.utils.getPackageInfoFromApk
import okhttp3.Request
import java.io.File
import java.net.HttpURLConnection

class FileDownloader() {
    private val logTag = "FileDownloader"
    private val bufferLengthBytes = 1024 * 8

    /**
     * Double needed as calculation can go ver Int.Max
     */
    private fun calculatePercentage(obtained: Double, total: Double) = (obtained * 100 / total).toInt()
    fun download(context: Context, storeAppData: StoreAppData, targetFile: File, callback: Callback) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "download() -> downloadUrl: ${storeAppData.downloadUrl}, targetFile: $targetFile")
        }

        callback.onStarted(storeAppData)

        if (targetFile.exists()) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "download() -> targetFile was already downloaded. Deleting it")
            }
            targetFile.delete()
        }

        try {
            val request = Request.Builder().url(storeAppData.downloadUrl).build()
            val response = HttpProvider.provideOkHttpClient().newCall(request).execute()
            val body = response.body
            val responseCode = response.code
            if (responseCode >= HttpURLConnection.HTTP_OK &&
                responseCode < HttpURLConnection.HTTP_MULT_CHOICE
            ) {
                val length = body.contentLength()
                body.byteStream().apply {
                    targetFile.outputStream().use { fileOut ->
                        var bytesCopied = 0
                        val buffer = ByteArray(bufferLengthBytes)
                        var bytes = read(buffer)
                        while (bytes >= 0) {
                            fileOut.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            bytes = read(buffer)

                            val percent = calculatePercentage(bytesCopied.toDouble(), length.toDouble())
                            if (CLog.isDebug()) {
                                CLog.log(logTag, "download() -> percent: $percent, bytesCopied: $bytesCopied, length: $length")
                            }
                            callback.onProgress(storeAppData, percent, bytesCopied, length)
                        }
                    }
                    if (CLog.isDebug()) {
                        CLog.log(logTag, "download() -> Completed")
                    }

                    val packageInfo = targetFile.getPackageInfoFromApk(context.applicationContext)
                    if (packageInfo != null) {
                        if (CLog.isDebug()) {
                            CLog.log(logTag, "download() -> Renaming completed. Emitting DownloadStatus.Completed")
                        }
                        callback.onCompleted(storeAppData, targetFile, packageInfo)
                    } else {
                        if (CLog.isDebug()) {
                            CLog.log(logTag, "download() -> Target file was malformed! Delete it")
                        }
                        targetFile.delete()
                        callback.onMalformedFileError(storeAppData)
                    }

                }
            } else {
                if (CLog.isDebug()) {
                    CLog.log(logTag, "download() -> Download error. responseCode: $responseCode")
                }
                callback.onServerError(storeAppData, responseCode)
            }
        } catch (e: Exception) {
            if (CLog.isDebug()) {
                CLog.logPrintStackTrace(e)
            }
            callback.onError(storeAppData, e)
        }
    }

    interface Callback {
        fun onStarted(storeAppData: StoreAppData)
        fun onProgress(storeAppData: StoreAppData, percent: Int, bytesCopied: Int, length: Long)
        fun onCompleted(storeAppData: StoreAppData, targetFile: File, packageInfo: PackageInfo)
        fun onError(storeAppData: StoreAppData, exception: Exception)
        fun onServerError(storeAppData: StoreAppData, responseCode: Int)
        fun onMalformedFileError(storeAppData: StoreAppData)
    }

    companion object {
        fun getDestinationFile(context: Context, storeAppData: StoreAppData): File {
            val extension = MimeTypeMap.getFileExtensionFromUrl(storeAppData.downloadUrl)
            val fileName = "${storeAppData.packageName}_${storeAppData.version}.$extension"
            return File(getBaseFolder(context), fileName)
        }

        fun getBaseFolder(context: Context): File {
            val childFolder = "apks"
            val baseFolder = File(context.externalCacheDir, childFolder)
            if (!baseFolder.exists()) {
                baseFolder.mkdirs()
            }
            return baseFolder
        }
    }

}