package io.github.solrudev.simpleinstaller.impl.installer

import android.os.Build
import io.github.solrudev.simpleinstaller.apksource.ApkSource
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.data.ProgressData
import io.github.solrudev.simpleinstaller.data.SessionOptions
import kotlinx.coroutines.flow.SharedFlow

/**
 * Minimum required interface to implement package installer.
 */
internal interface MinimalPackageInstaller {
	val progress: SharedFlow<ProgressData>
	suspend fun installPackages(apkFiles: Array<out ApkSource>, options: SessionOptions): InstallResult
}

/**
 * Returns [MinimalPackageInstaller] for current API level.
 */
@JvmSynthetic
internal fun MinimalPackageInstaller(): MinimalPackageInstaller {
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
		return PackageInstallerApi16
	}
	return PackageInstallerApi21
}