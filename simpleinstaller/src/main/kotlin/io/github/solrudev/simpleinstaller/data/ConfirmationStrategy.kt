package io.github.solrudev.simpleinstaller.data

import io.github.solrudev.simpleinstaller.data.ConfirmationStrategy.DEFERRED
import io.github.solrudev.simpleinstaller.data.ConfirmationStrategy.IMMEDIATE

/**
 * A strategy for handling user's confirmation of installation or uninstallation.
 *
 * * [IMMEDIATE] &mdash; user will be prompted to confirm installation or uninstallation right away. Suitable for
 * 	 launching session directly from the UI when app is in foreground.
 * * [DEFERRED] (default) &mdash; user will be shown a high-priority notification (full-screen intent) which will launch
 *   confirmation activity.
 */
public enum class ConfirmationStrategy {

	/**
	 * Prompt user to confirm installation or uninstallation right away.
	 */
	IMMEDIATE,

	/**
	 * Show a high-priority notification (full-screen intent) which will launch confirmation activity to a user.
	 */
	DEFERRED
}