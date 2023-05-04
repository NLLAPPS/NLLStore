package com.nll.store.activityresult

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract

 class InstallPermissionContract(private val context: Context) : ActivityResultContract<Unit, Boolean>() {

	 override fun createIntent(context: Context, input: Unit): Intent = Intent(
		Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
		Uri.parse("package:${context.packageName}")
	)

	 override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
		context.packageManager.canRequestPackageInstalls()
}