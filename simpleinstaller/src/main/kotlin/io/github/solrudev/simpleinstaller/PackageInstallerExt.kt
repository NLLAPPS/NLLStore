package io.github.solrudev.simpleinstaller

import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.solrudev.simpleinstaller.apksource.ApkSource
import io.github.solrudev.simpleinstaller.apksource.AssetFileDescriptorApkSource
import io.github.solrudev.simpleinstaller.apksource.FileApkSource
import io.github.solrudev.simpleinstaller.apksource.UriApkSource
import io.github.solrudev.simpleinstaller.apksource.utils.toApkSourceArray
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.data.SessionOptions
import io.github.solrudev.simpleinstaller.exceptions.SplitPackagesNotSupportedException
import io.github.solrudev.simpleinstaller.exceptions.UnsupportedUriSchemeException
import io.github.solrudev.simpleinstaller.utils.extensions.isSupported
import java.io.File

/**
 * Starts an install session and suspends until it finishes.
 *
 * Split packages are not supported on Android versions lower than Lollipop (5.0).
 * Attempting to use this method on these versions will produce [SplitPackagesNotSupportedException].
 *
 * It is safe to call on main thread. Supports cancellation.
 *
 * @see PackageInstaller.installSplitPackage
 * @param apkFiles Any source of split APK files implemented by [ApkSource].
 * @param optionsBuilder [SessionOptions] initializer.
 * @return [InstallResult]
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@JvmSynthetic
public suspend inline fun PackageInstaller.installSplitPackage(
	vararg apkFiles: ApkSource,
	optionsBuilder: SessionOptions.Builder.() -> Unit
): InstallResult = installSplitPackage(apkFiles = apkFiles, SessionOptions(optionsBuilder))

/**
 * Accepts an array of [Uri] objects, converts it to an array of [ApkSource] objects and calls
 * [PackageInstaller]'s `installSplitPackage()`.
 *
 * @see PackageInstaller.installSplitPackage
 * @param apkFiles [Uri] objects representing split APK files. Must be file: or content: URIs.
 * @param optionsBuilder [SessionOptions] initializer. If empty [SessionOptions.DEFAULT] is used.
 * @return [InstallResult]
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@JvmSynthetic
public suspend inline fun PackageInstaller.installSplitPackage(
	vararg apkFiles: Uri,
	optionsBuilder: SessionOptions.Builder.() -> Unit = {}
): InstallResult {
	apkFiles.forEach {
		if (!it.isSupported) {
			throw UnsupportedUriSchemeException(it)
		}
	}
	return installSplitPackage(apkFiles = apkFiles.toApkSourceArray(), SessionOptions(optionsBuilder))
}

/**
 * Accepts an array of [AssetFileDescriptor] objects, converts it to an array of [ApkSource] objects and calls
 * [PackageInstaller]'s `installSplitPackage()`.
 *
 * @see PackageInstaller.installSplitPackage
 * @param apkFiles [AssetFileDescriptor] objects representing split APK files.
 * @param optionsBuilder [SessionOptions] initializer. If empty [SessionOptions.DEFAULT] is used.
 * @return [InstallResult]
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@JvmSynthetic
public suspend inline fun PackageInstaller.installSplitPackage(
	vararg apkFiles: AssetFileDescriptor,
	optionsBuilder: SessionOptions.Builder.() -> Unit = {}
): InstallResult = installSplitPackage(apkFiles = apkFiles.toApkSourceArray(), SessionOptions(optionsBuilder))

/**
 * Accepts an array of [File] objects, converts it to an array of [ApkSource] objects and calls
 * [PackageInstaller]'s `installSplitPackage()`.
 *
 * @see PackageInstaller.installSplitPackage
 * @param apkFiles [File] objects representing split APK files.
 * @param optionsBuilder [SessionOptions] initializer. If empty [SessionOptions.DEFAULT] is used.
 * @return [InstallResult]
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@JvmSynthetic
public suspend inline fun PackageInstaller.installSplitPackage(
	vararg apkFiles: File,
	optionsBuilder: SessionOptions.Builder.() -> Unit = {}
): InstallResult = installSplitPackage(apkFiles = apkFiles.toApkSourceArray(), SessionOptions(optionsBuilder))

/**
 * Starts an install session and suspends until it finishes.
 *
 * It is safe to call on main thread. Supports cancellation.
 *
 * @see PackageInstaller.installPackage
 * @param apkFile Any source of APK file implemented by [ApkSource].
 * @param optionsBuilder [SessionOptions] initializer.
 * @return [InstallResult]
 */
@JvmSynthetic
public suspend inline fun PackageInstaller.installPackage(
	apkFile: ApkSource,
	optionsBuilder: SessionOptions.Builder.() -> Unit
): InstallResult = installPackage(apkFile, SessionOptions(optionsBuilder))

/**
 * Accepts a [Uri], converts it to an [ApkSource] and calls [PackageInstaller]'s `installPackage()`.
 *
 * @see PackageInstaller.installPackage
 * @param apkFile [Uri] object representing APK file. Must be a file: or content: URI.
 * @param optionsBuilder [SessionOptions] initializer. If empty [SessionOptions.DEFAULT] is used.
 * @return [InstallResult]
 */
@JvmSynthetic
public suspend inline fun PackageInstaller.installPackage(
	apkFile: Uri,
	optionsBuilder: SessionOptions.Builder.() -> Unit = {}
): InstallResult = installPackage(UriApkSource(apkFile), SessionOptions(optionsBuilder))

/**
 * Accepts an [AssetFileDescriptor], converts it to an [ApkSource] and calls [PackageInstaller]'s `installPackage()`.
 *
 * @see PackageInstaller.installPackage
 * @param apkFile [AssetFileDescriptor] object representing APK file.
 * @param optionsBuilder [SessionOptions] initializer. If empty [SessionOptions.DEFAULT] is used.
 * @return [InstallResult]
 */
@JvmSynthetic
public suspend inline fun PackageInstaller.installPackage(
	apkFile: AssetFileDescriptor,
	optionsBuilder: SessionOptions.Builder.() -> Unit = {}
): InstallResult = installPackage(AssetFileDescriptorApkSource(apkFile), SessionOptions(optionsBuilder))

/**
 * Accepts a [File], converts it to an [ApkSource] and calls [PackageInstaller]'s `installPackage()`.
 *
 * @see PackageInstaller.installPackage
 * @param apkFile [File] object representing APK file.
 * @param optionsBuilder [SessionOptions] initializer. If empty [SessionOptions.DEFAULT] is used.
 * @return [InstallResult]
 */
@JvmSynthetic
public suspend inline fun PackageInstaller.installPackage(
	apkFile: File,
	optionsBuilder: SessionOptions.Builder.() -> Unit = {}
): InstallResult = installPackage(FileApkSource(apkFile), SessionOptions(optionsBuilder))