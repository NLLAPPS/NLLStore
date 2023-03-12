package io.github.solrudev.simpleinstaller.exceptions

import android.os.Build

public class SplitPackagesNotSupportedException : Exception() {
	public override val message: String =
		"Split packages are not supported on current Android API level: ${Build.VERSION.SDK_INT}."
}