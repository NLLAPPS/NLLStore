package com.nll.store.utils

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

object ApiLevel {

    /**
     * Api Level 30+, Android 11
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun isRPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    /**
     * Api Level 31+, Android 12
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun isSPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /**
     * Api Level 32+, Android 12L
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S_V2)
    fun isSV2Plus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2

    /**
     * Api Level 33+, Android 13
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    fun isTPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * Api Level 34+, Android 14
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun isUpsideDownCakePlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
}