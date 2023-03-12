package io.github.solrudev.simpleinstaller.activityresult

import android.os.Build
import androidx.activity.result.contract.ActivityResultContract

/**
 * Returns package uninstall [ActivityResultContract] for current API level.
 */
@Suppress("FunctionName")
@JvmSynthetic
internal fun UninstallPackageContract(): ActivityResultContract<String, Boolean> {
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
		return ActionUninstallPackageContract()
	}
	return ActionDeletePackageContract()
}