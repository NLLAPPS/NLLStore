package io.github.solrudev.simpleinstaller.apksource

import android.content.res.AssetFileDescriptor
import android.net.Uri
import java.io.FileInputStream

/**
 * [ApkSource] implementation for [AssetFileDescriptor].
 */
public class AssetFileDescriptorApkSource(private val apkAssetFileDescriptor: AssetFileDescriptor) : ApkSource() {

	public override val length: Long
		get() = apkAssetFileDescriptor.declaredLength

	public override fun openInputStream(): FileInputStream? = apkAssetFileDescriptor.createInputStream()
	public override suspend fun getUri(): Uri = createTempCopy()
}