package com.nll.store.utils

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

fun Int.extHumanReadableByteCount(si: Boolean): String {
    val unit = if (si) 1000 else 1024
    if (this < unit)
        return "$this B"
    val exp = (ln(this.toDouble()) / ln(unit.toDouble())).toInt()
    val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
    return String.format(Locale.getDefault(), "%.1f %sB", this / unit.toDouble().pow(exp.toDouble()), pre)
}