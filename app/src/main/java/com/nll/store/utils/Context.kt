package com.nll.store.utils

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.getSystemService
import com.nll.store.log.CLog

fun Context.extConnectivityManager(): ConnectivityManager? = getSystemService()
/**
 * If @param errorMessage is provided, a toast message with provided  be shown on failure to start activity
 */
fun Context.extTryStartActivity(intent: Intent, errorMessage: String? = null) {
    try {
        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK == 0) {
            if (CLog.isDebug()) {
                CLog.log("Context.extTryStartActivity", "FLAG_ACTIVITY_NEW_TASK was not added! Adding it")
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    } catch (e: Exception) {
        errorMessage?.let {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
        CLog.logPrintStackTrace(e)
    }
}
@ColorInt
fun Context.extGetThemeAttrColor(@AttrRes colorAttr: Int): Int {
    val array = obtainStyledAttributes(null, intArrayOf(colorAttr))
    return try {
        array.getColor(0, 0)
    } finally {
        array.recycle()
    }
}