package com.nll.store.installer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import com.nll.store.log.CLog
import com.nll.store.utils.ApiLevel
import com.nll.store.utils.extGetParcelableExtra

class InstallationEventsReceiver : BroadcastReceiver() {
    private val logTag ="InstallationEventsReceiver"

    companion object{
        private const val intentAction = "APP_INSTALLER_ACTION"
        private const val requestCode = 6541
        fun createIntentSender(context: Context): IntentSender {
            val intent = Intent(context, InstallationEventsReceiver::class.java).apply {
                action = intentAction
                //https://cs.android.com/android/platform/superproject/+/master:frameworks/base/packages/PackageInstaller/src/com/android/packageinstaller/InstallInstalling.java;drc=143db55921ddda50741c90e0f8258b77298e4a9f;l=365
                flags = Intent.FLAG_RECEIVER_FOREGROUND
            }
            val flags = if (ApiLevel.isSPlus()) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
            return pendingIntent.intentSender
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "onReceive() -> intent $intent")
        }
        if (intent?.action != intentAction) {
            return
        }

        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        if (CLog.isDebug()) {
            CLog.log(logTag, "onReceive() -> sessionId $sessionId")
        }

        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                if (CLog.isDebug()) {
                    CLog.log(logTag, "onReceive() -> status: STATUS_PENDING_USER_ACTION")
                }
                val confirmationIntent = intent.extGetParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirmationIntent != null) {

                    // This app isn't privileged, so the user has to confirm the install.
                    val wrapperIntent = confirmationIntent.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(wrapperIntent)
                }
            }

            else -> {

                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                val otherPackageName = intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)
                val storagePath = intent.getStringExtra(PackageInstaller.EXTRA_STORAGE_PATH)
                if (CLog.isDebug()) {
                    CLog.log(logTag, "onReceive() -> status: $status, message: $message, otherPackageName: $otherPackageName, storagePath: $storagePath")
                }
                when (val installResult = PackageInstallResult.fromStatusCode(status, message, otherPackageName, storagePath)) {
                    is PackageInstallResult.Failure -> AppInstallManager.updateInstallState(InstallationState.Install.Completed(PackageInstallResult.Failure(installResult.cause)))
                    PackageInstallResult.Success -> AppInstallManager.updateInstallState(InstallationState.Install.Completed(PackageInstallResult.Success))
                }
            }
        }
    }
}