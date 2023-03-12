package io.github.solrudev.simpleinstaller.apksource

import android.content.ContentResolver
import android.net.Uri
import io.github.solrudev.simpleinstaller.SimpleInstaller.applicationContext
import io.github.solrudev.simpleinstaller.exceptions.UnsupportedUriSchemeException
import io.github.solrudev.simpleinstaller.utils.extensions.length
import java.io.File
import java.io.InputStream

/**
 * [ApkSource] implementation for [Uri].
 */
public class UriApkSource(private val apkUri: Uri) : ApkSource() {

	public override val length: Long
		get() = apkUri.length

	public override val file: File
		get() = when (apkUri.scheme) {
			ContentResolver.SCHEME_FILE -> File(apkUri.path ?: "")
			else -> super.file
		}

	public override fun openInputStream(): InputStream? = applicationContext.contentResolver.openInputStream(apkUri)

	public override suspend fun getUri(): Uri = when (apkUri.scheme) {
		ContentResolver.SCHEME_CONTENT -> createTempCopy()
		ContentResolver.SCHEME_FILE -> apkUri
		else -> throw UnsupportedUriSchemeException(apkUri)
	}
}