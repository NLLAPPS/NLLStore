package com.nll.store.installer

import android.content.pm.PackageInstaller

sealed class PackageInstallResult {

    data object Success : PackageInstallResult()

    /**
     * Install failed.
     *
     * May contain cause of failure in [cause] property.
     * @property cause Cause of installation failure. Always null on Android versions lower than Lollipop (5.0).
     */
    data class Failure(val cause: PackageInstallFailureCause? = null) : PackageInstallResult() {

        override fun toString(): String = "INSTALL_FAILURE${if (cause != null) " | cause = $cause" else ""}"
    }

    companion object {

        /**
         * Converts Android's [PackageInstaller] status code to [InstallResult] object.
         */

        fun fromStatusCode(
            statusCode: Int,
            message: String? = null,
            otherPackageName: String? = null,
            storagePath: String? = null
        ): PackageInstallResult = when (statusCode) {
            PackageInstaller.STATUS_SUCCESS -> Success
            PackageInstaller.STATUS_FAILURE -> Failure(PackageInstallFailureCause.Generic(message))
            PackageInstaller.STATUS_FAILURE_ABORTED -> Failure(PackageInstallFailureCause.Aborted(message))
            PackageInstaller.STATUS_FAILURE_BLOCKED -> Failure(PackageInstallFailureCause.Blocked(message, otherPackageName))
            PackageInstaller.STATUS_FAILURE_CONFLICT -> Failure(PackageInstallFailureCause.Conflict(message, otherPackageName))
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> Failure(PackageInstallFailureCause.Incompatible(message))
            PackageInstaller.STATUS_FAILURE_INVALID -> Failure(PackageInstallFailureCause.Invalid(message))
            PackageInstaller.STATUS_FAILURE_STORAGE -> Failure(PackageInstallFailureCause.Storage(message, storagePath))
            else -> Failure()
        }
    }
}