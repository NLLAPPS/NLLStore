package io.github.solrudev.simpleinstaller.utils.extensions

import android.content.Intent
import android.os.Build
import android.os.Parcelable

@Suppress("DEPRECATION")
@JvmSynthetic
internal inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(name: String): T? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		getParcelableExtra(name, T::class.java)
	} else {
		getParcelableExtra(name)
	}
}