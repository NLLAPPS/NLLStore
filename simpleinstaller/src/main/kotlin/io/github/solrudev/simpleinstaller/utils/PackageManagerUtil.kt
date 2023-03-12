package io.github.solrudev.simpleinstaller.utils

import android.content.pm.PackageManager
import io.github.solrudev.simpleinstaller.SimpleInstaller.packageManager
import io.github.solrudev.simpleinstaller.utils.extensions.getApplicationInfoCompat
import io.github.solrudev.simpleinstaller.utils.extensions.getPackageArchiveInfoCompat
import io.github.solrudev.simpleinstaller.utils.extensions.getPackageInfoCompat
import java.io.File

@JvmSynthetic
internal fun isPackageInstalled(packageName: String) = try {
	packageManager.getPackageInfoCompat(packageName, PackageManager.GET_ACTIVITIES)
	true
} catch (_: PackageManager.NameNotFoundException) {
	false
}

@JvmSynthetic
internal fun getApplicationLabel(packageName: String) = try {
	packageManager
		.getApplicationInfoCompat(packageName, PackageManager.GET_META_DATA)
		.loadLabel(packageManager)
} catch (_: PackageManager.NameNotFoundException) {
	null
}

@JvmSynthetic
internal fun getApplicationLabel(apkFile: File) = apkFile.run {
	val packageInfo = packageManager.getPackageArchiveInfoCompat(absolutePath, PackageManager.GET_META_DATA)
	packageInfo?.applicationInfo
		?.loadLabel(packageManager)
		?.takeIf { it != packageInfo.packageName }
}