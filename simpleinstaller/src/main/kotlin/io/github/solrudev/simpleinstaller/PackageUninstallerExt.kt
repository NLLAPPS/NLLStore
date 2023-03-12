package io.github.solrudev.simpleinstaller

import io.github.solrudev.simpleinstaller.data.SessionOptions

/**
 * Starts a uninstall session and suspends until it finishes.
 *
 * It is safe to call on main thread. Supports cancellation.
 *
 * @see PackageUninstaller.uninstallPackage
 * @param packageName Name of the package to be uninstalled.
 * @param optionsBuilder [SessionOptions] initializer.
 * @return Success of uninstallation.
 */
@JvmSynthetic
public suspend inline fun PackageUninstaller.uninstallPackage(
	packageName: String,
	optionsBuilder: SessionOptions.Builder.() -> Unit
): Boolean = uninstallPackage(packageName, SessionOptions(optionsBuilder))