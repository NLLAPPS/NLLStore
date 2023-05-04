package com.nll.store.installer

import android.content.Context
import java.io.InputStream

interface ApkSource {

    fun getInputStream(context: Context): InputStream

    fun getLength(context: Context): Long
}