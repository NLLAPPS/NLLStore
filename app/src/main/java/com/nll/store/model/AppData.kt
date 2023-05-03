package com.nll.store.model


import android.widget.ImageView
import com.nll.store.BuildConfig

data class AppData(val storeAppData: StoreAppData, val appInstallState: AppInstallState) {


    fun getId() = storeAppData.getId()

    fun loadIcon(imageView: ImageView) {

        when (appInstallState) {
            is AppInstallState.Installed -> imageView.setImageDrawable(appInstallState.localAppData.icon)
            AppInstallState.NotInstalled -> storeAppData.loadLogo(imageView)

        }
    }

    fun canBeUpdated() = when (appInstallState) {
        is AppInstallState.Installed -> storeAppData.version > appInstallState.localAppData.versionCode
        AppInstallState.NotInstalled -> false

    }

    fun isSelf() = storeAppData.packageName == BuildConfig.APPLICATION_ID
}