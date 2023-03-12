package io.github.solrudev.simpleinstaller.exceptions

import android.net.Uri

public class UnsupportedUriSchemeException(uri: Uri) : Exception() {
	public override val message: String = "Scheme of the provided URI is not supported: $uri"
}