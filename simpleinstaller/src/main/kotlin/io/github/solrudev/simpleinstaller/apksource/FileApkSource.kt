package io.github.solrudev.simpleinstaller.apksource

import android.net.Uri
import java.io.File
import java.io.FileInputStream

/**
 * [ApkSource] implementation for [File].
 */
public class FileApkSource(private val apkFile: File) : ApkSource() {

	public override val length: Long
		get() = apkFile.length()

	public override val file: File
		get() = apkFile

	public override fun openInputStream(): FileInputStream = FileInputStream(apkFile)
	public override suspend fun getUri(): Uri = Uri.fromFile(apkFile)
}