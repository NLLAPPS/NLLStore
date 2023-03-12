package io.github.solrudev.simpleinstaller.utils.extensions

import android.os.Build
import android.os.Bundle
import android.os.Parcelable

@Suppress("DEPRECATION")
@JvmSynthetic
internal inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getParcelable(key, T::class.java)
	} else {
		getParcelable(key)
	}
}