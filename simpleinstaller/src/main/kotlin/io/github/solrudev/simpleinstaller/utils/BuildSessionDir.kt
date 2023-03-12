package io.github.solrudev.simpleinstaller.utils

// Reference code:
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/pm/PackageInstallerService.java;l=950;drc=bfd98fb906398de6487ed484cb8abd0f615c7ea8
// User-initiated sessions install APKs, not APEXs, so they're always not staged and don't have INSTALL_APEX flag

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File

private const val ENV_ANDROID_DATA = "ANDROID_DATA"
private val DIR_ANDROID_DATA_PATH = System.getenv(ENV_ANDROID_DATA) ?: "/data"
private val DIR_ANDROID_DATA = File(DIR_ANDROID_DATA_PATH)

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@JvmSynthetic
internal fun buildSessionDir(sessionId: Int): File {
	val sessionStagingDir = File(DIR_ANDROID_DATA, "app")
	return File(sessionStagingDir, "vmdl$sessionId.tmp")
}