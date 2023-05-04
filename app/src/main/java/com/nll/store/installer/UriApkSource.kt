package com.nll.store.installer

import android.content.Context
import android.net.Uri


class UriApkSource(private val uri: Uri) : ApkSource {
    /**
     * We expect openInputStream return not null because we already check if we have read access when passing uri to installer
     */
    override fun getInputStream(context: Context) =  context.contentResolver.openInputStream(uri)!!

    /**
     * We expect openInputStream return not null because we already check if we have read access when passing uri to installer
     */
    override fun getLength(context: Context) = context.contentResolver.openFileDescriptor(uri, "r").use { it!!.statSize }
}