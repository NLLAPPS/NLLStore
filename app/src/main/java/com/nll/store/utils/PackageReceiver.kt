package com.nll.store.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nll.store.log.CLog


class PackageReceiver(private val callBack: (String) -> Unit) : BroadcastReceiver() {
    private val logTag = "PackageReceiver"
    override fun onReceive(context: Context, intent: Intent?) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "onReceive() -> intent $intent")
        }
        if (intent?.action == Intent.ACTION_PACKAGE_REMOVED) {
            val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            val packageName = intent.data?.schemeSpecificPart
            if (CLog.isDebug()) {
                CLog.log(logTag, "onReceive() -> isReplacing: $isReplacing, packageName: $packageName")
            }
            if(!isReplacing && packageName != null){
                callBack.invoke(packageName)
            }

        } else if (intent?.action == Intent.ACTION_PACKAGE_ADDED) {
            val packageName = intent.data?.schemeSpecificPart
            if (packageName != null) {
                if (CLog.isDebug()) {
                    CLog.log(logTag, "onReceive() -> Name of package changed: packageName: $packageName")
                }
                callBack.invoke(packageName)
            }
        }
    }
}