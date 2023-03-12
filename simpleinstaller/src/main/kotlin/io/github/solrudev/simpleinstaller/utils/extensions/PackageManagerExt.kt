package io.github.solrudev.simpleinstaller.utils.extensions

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

@Suppress("DEPRECATION")
@JvmSynthetic
internal fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int): PackageInfo {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
	} else {
		getPackageInfo(packageName, flags)
	}
}

@Suppress("DEPRECATION")
@JvmSynthetic
internal fun PackageManager.getApplicationInfoCompat(packageName: String, flags: Int): ApplicationInfo {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
	} else {
		getApplicationInfo(packageName, flags)
	}
}

@Suppress("DEPRECATION")
@JvmSynthetic
internal fun PackageManager.getPackageArchiveInfoCompat(absolutePath: String, flags: Int): PackageInfo? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getPackageArchiveInfo(absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
	} else {
		getPackageArchiveInfo(absolutePath, flags)
	}
}