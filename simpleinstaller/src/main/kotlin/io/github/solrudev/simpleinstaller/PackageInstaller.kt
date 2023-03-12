package io.github.solrudev.simpleinstaller

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.solrudev.simpleinstaller.apksource.ApkSource
import io.github.solrudev.simpleinstaller.data.InstallFailureCause
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.data.ProgressData
import io.github.solrudev.simpleinstaller.data.SessionOptions
import io.github.solrudev.simpleinstaller.exceptions.SplitPackagesNotSupportedException
import io.github.solrudev.simpleinstaller.impl.installer.MinimalPackageInstaller
import io.github.solrudev.simpleinstaller.impl.installer.PackageInstallerWrapper
import kotlinx.coroutines.flow.SharedFlow

/**
 * Provides Android packages install functionality.
 */
public interface PackageInstaller {

	/**
	 * Property which reflects if there's already an install session going on.
	 * If returns true, attempting to start a new install session will result in [IllegalStateException].
	 */
	public val hasActiveSession: Boolean

	/**
	 * A [SharedFlow] of [ProgressData] which represents installation progress.
	 */
	@get:JvmSynthetic
	public val progress: SharedFlow<ProgressData>

	/**
	 * Starts an install session and suspends until it finishes.
	 *
	 * Split packages are not supported on Android versions lower than Lollipop (5.0).
	 * Attempting to use this method on these versions will produce [SplitPackagesNotSupportedException].
	 *
	 * It is safe to call on main thread. Supports cancellation.
	 *
	 * @param apkFiles Any source of split APK files implemented by [ApkSource].
	 * @param options Options for install session. [SessionOptions.DEFAULT] is default value.
	 * @return [InstallResult]
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	@JvmSynthetic
	public suspend fun installSplitPackage(
		vararg apkFiles: ApkSource,
		options: SessionOptions = SessionOptions.DEFAULT
	): InstallResult

	/**
	 * Starts an install session and suspends until it finishes.
	 *
	 * It is safe to call on main thread. Supports cancellation.
	 *
	 * @param apkFile Any source of APK file implemented by [ApkSource].
	 * @param options Options for install session. [SessionOptions.DEFAULT] is default value.
	 * @return [InstallResult]
	 */
	@JvmSynthetic
	public suspend fun installPackage(
		apkFile: ApkSource,
		options: SessionOptions = SessionOptions.DEFAULT
	): InstallResult

	/**
	 * Asynchronously starts an install session with split packages and delivers result and progress updates via
	 * [callback].
	 *
	 * Split packages are not supported on Android versions lower than Lollipop (5.0).
	 * Attempting to use this method on these versions will produce [SplitPackagesNotSupportedException].
	 *
	 * @param apkFiles Any source of split APK files implemented by [ApkSource].
	 * @param options Options for install session. Use [SessionOptions.DEFAULT] as a default value.
	 * @param callback A callback object implementing [PackageInstaller.Callback] interface.
	 * Its methods are called on main thread.
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	public fun installSplitPackage(
		vararg apkFiles: ApkSource,
		options: SessionOptions,
		callback: Callback
	)

	/**
	 * Asynchronously starts an install session and delivers result and progress updates via [callback].
	 *
	 * @param apkFile Any source of APK file implemented by [ApkSource].
	 * @param options Options for install session. Use [SessionOptions.DEFAULT] as a default value.
	 * @param callback A callback object implementing [PackageInstaller.Callback] interface.
	 * Its methods are called on main thread.
	 */
	public fun installPackage(
		apkFile: ApkSource,
		options: SessionOptions,
		callback: Callback
	)

	/**
	 * Cancels current coroutine and abandons current install session. Does nothing if install session was started with
	 * suspending functions.
	 */
	public fun cancel()

	/**
	 * Default singleton instance of [PackageInstaller].
	 */
	public companion object : PackageInstaller by PackageInstallerWrapper(MinimalPackageInstaller()) {

		/**
		 * Retrieves the default singleton instance of [PackageInstaller].
		 */
		@JvmStatic
		public fun getInstance(): PackageInstaller = this
	}

	/**
	 * A callback interface for [PackageInstaller]'s usage from Java.
	 */
	public interface Callback {

		/**
		 * Called when installation finished with success.
		 */
		public fun onSuccess() {}

		/**
		 * Called when installation finished with failure.
		 * @param cause Cause of failure. Always null on Android versions lower than Lollipop (5.0).
		 */
		public fun onFailure(cause: InstallFailureCause?) {}

		/**
		 * Called when an exception is thrown.
		 * @param exception A [Throwable] which has been thrown.
		 */
		public fun onException(exception: Throwable) {}

		/**
		 * Called when installation was canceled normally.
		 */
		public fun onCanceled() {}

		/**
		 * Called when installation progress has been updated.
		 * @param progress [ProgressData] object representing installation progress.
		 */
		public fun onProgressChanged(progress: ProgressData) {}
	}
}