package com.nll.store.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

fun PackageManager.getInstalledApplicationsCompat(flags: Int): List<ApplicationInfo> {
	return getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
}