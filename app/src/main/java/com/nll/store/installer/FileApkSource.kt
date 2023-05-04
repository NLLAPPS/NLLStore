package com.nll.store.installer

import android.content.Context
import java.io.File
import java.io.FileInputStream


class FileApkSource(private val file: File) : ApkSource {
    override fun getInputStream(context: Context) = FileInputStream(file)
    override fun getLength(context: Context) = file.length()
}