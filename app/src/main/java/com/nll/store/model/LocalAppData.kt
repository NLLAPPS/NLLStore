package com.nll.store.model

import android.graphics.drawable.Drawable

data class LocalAppData(val id: Int, val icon: Drawable, val name: String, val packageName: String, val versionCode: Long)
