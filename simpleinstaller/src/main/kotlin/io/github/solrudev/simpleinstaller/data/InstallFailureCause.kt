package io.github.solrudev.simpleinstaller.data

/**
 * Represents the cause of installation failure. Contains string representation in [message] property.
 *
 * May be either [Generic], [Aborted], [Blocked], [Conflict], [Incompatible], [Invalid] or [Storage].
 * @property message Detailed string representation of the status, including raw details that are useful for debugging.
 */
public sealed class InstallFailureCause(public open val message: String?) {

	/**
	 * The operation failed in a generic way. The system will always try to provide a more specific failure reason,
	 * but in some rare cases this may be delivered.
	 */
	public data class Generic(public override val message: String?) : InstallFailureCause(message) {
		public override fun toString(): String = message ?: "INSTALL_FAILURE"
	}

	/**
	 * The operation failed because it was actively aborted.
	 * For example, the user actively declined requested permissions, or the session was abandoned.
	 */
	public data class Aborted(public override val message: String?) : InstallFailureCause(message) {
		public override fun toString(): String = message ?: "INSTALL_FAILURE_ABORTED"
	}

	/**
	 * The operation failed because it was blocked. For example, a device policy may be blocking the operation,
	 * a package verifier may have blocked the operation, or the app may be required for core system operation.
	 *
	 * The result may also contain [otherPackageName] with the specific package blocking the install.
	 */
	public data class Blocked(
		public override val message: String?,
		public val otherPackageName: String? = null
	) : InstallFailureCause(message) {

		public override fun toString(): String = (message ?: "INSTALL_FAILURE_BLOCKED") +
				if (otherPackageName != null) " | OTHER_PACKAGE_NAME = $otherPackageName" else ""
	}

	/**
	 * The operation failed because it conflicts (or is inconsistent with) with another package already installed
	 * on the device. For example, an existing permission, incompatible certificates, etc. The user may be able to
	 * uninstall another app to fix the issue.
	 *
	 * The result may also contain [otherPackageName] with the specific package identified as the cause of the conflict.
	 */
	public data class Conflict(
		public override val message: String?,
		public val otherPackageName: String? = null
	) : InstallFailureCause(message) {

		public override fun toString(): String = (message ?: "INSTALL_FAILURE_CONFLICT") +
				if (otherPackageName != null) " | OTHER_PACKAGE_NAME = $otherPackageName" else ""
	}

	/**
	 * The operation failed because it is fundamentally incompatible with this device. For example, the app may
	 * require a hardware feature that doesn't exist, it may be missing native code for the ABIs supported by the
	 * device, or it requires a newer SDK version, etc.
	 */
	public data class Incompatible(public override val message: String?) : InstallFailureCause(message) {
		public override fun toString(): String = message ?: "INSTALL_FAILURE_INCOMPATIBLE"
	}

	/**
	 * The operation failed because one or more of the APKs was invalid. For example, they might be malformed,
	 * corrupt, incorrectly signed, mismatched, etc.
	 */
	public data class Invalid(public override val message: String?) : InstallFailureCause(message) {
		public override fun toString(): String = message ?: "INSTALL_FAILURE_INVALID"
	}

	/**
	 * The operation failed because of storage issues. For example, the device may be running low on space,
	 * or external media may be unavailable. The user may be able to help free space or insert different external media.
	 *
	 * The result may also contain [storagePath] with the path to the storage device that caused the failure.
	 */
	public data class Storage(
		public override val message: String?,
		public val storagePath: String? = null
	) : InstallFailureCause(message) {

		public override fun toString(): String = (message ?: "INSTALL_FAILURE_STORAGE") +
				if (storagePath != null) " | STORAGE_PATH = $storagePath" else ""
	}
}