package io.github.solrudev.simpleinstaller

import io.github.solrudev.simpleinstaller.data.SessionOptions
import io.github.solrudev.simpleinstaller.impl.PackageUninstallerImpl

/**
 * Provides Android packages uninstall functionality.
 */
public interface PackageUninstaller {

	/**
	 * Property which reflects if there's already a uninstall session going on.
	 * If returns true, attempting to start a new uninstall session will result in [IllegalStateException].
	 */
	public val hasActiveSession: Boolean

	/**
	 * Starts a uninstall session and suspends until it finishes.
	 *
	 * It is safe to call on main thread. Supports cancellation.
	 *
	 * @param packageName Name of the package to be uninstalled.
	 * @param options Options for uninstall session. [SessionOptions.DEFAULT] is default value.
	 * @return Success of uninstallation.
	 */
	@JvmSynthetic
	public suspend fun uninstallPackage(packageName: String, options: SessionOptions = SessionOptions.DEFAULT): Boolean

	/**
	 * Asynchronously starts a uninstall session and delivers result via [callback].
	 *
	 * @param packageName Name of the package to be uninstalled.
	 * @param options Options for uninstall session. Use [SessionOptions.DEFAULT] as a default value.
	 * @param callback A callback object implementing [PackageUninstaller.Callback] interface.
	 * Its methods are called on main thread.
	 */
	public fun uninstallPackage(packageName: String, options: SessionOptions, callback: Callback)

	/**
	 * Cancels current coroutine and uninstall session. Does nothing if uninstall session was started with suspending
	 * function.
	 */
	public fun cancel()

	/**
	 * Default singleton instance of [PackageUninstaller].
	 */
	public companion object : PackageUninstaller by PackageUninstallerImpl {

		/**
		 * Retrieves the default singleton instance of [PackageUninstaller].
		 */
		@JvmStatic
		public fun getInstance(): PackageUninstaller = PackageUninstallerImpl
	}

	/**
	 * A callback interface for [PackageUninstaller]'s usage from Java.
	 */
	public interface Callback {

		/**
		 * Called when uninstallation finished, either with success or failure.
		 * @param success Success of uninstallation.
		 */
		public fun onFinished(success: Boolean) {}

		/**
		 * Called when an exception is thrown.
		 * @param exception A [Throwable] which has been thrown.
		 */
		public fun onException(exception: Throwable) {}

		/**
		 * Called when uninstallation was canceled normally.
		 */
		public fun onCanceled() {}
	}
}