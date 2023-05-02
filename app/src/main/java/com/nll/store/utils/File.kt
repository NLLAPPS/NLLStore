package com.nll.store.utils

import android.content.Context
import android.content.pm.PackageInfo
import com.nll.store.log.CLog
import java.io.File

fun File.getPackageInfoFromApk(context: Context): PackageInfo? {
    return try {
        context.packageManager.getPackageArchiveInfo(absolutePath, 0)
    } catch (e: Exception) {
        CLog.logPrintStackTrace(e)
        null
    }
}