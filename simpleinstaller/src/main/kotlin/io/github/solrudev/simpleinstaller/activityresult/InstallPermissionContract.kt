package io.github.solrudev.simpleinstaller.activityresult

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import io.github.solrudev.simpleinstaller.SimpleInstaller.installerPackageName
import io.github.solrudev.simpleinstaller.SimpleInstaller.packageManager

/**
 * An [ActivityResultContract] to request install permission.
 */
@RequiresApi(Build.VERSION_CODES.O)
public class InstallPermissionContract : ActivityResultContract<Unit, Boolean>() {

	public override fun createIntent(context: Context, input: Unit): Intent = Intent(
		Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
		Uri.parse("package:$installerPackageName")
	)

	public override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
		packageManager.canRequestPackageInstalls()
}