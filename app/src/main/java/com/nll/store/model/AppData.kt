package com.nll.store.model

import android.widget.ImageView

data class AppData(val storeAppData: StoreAppData, val localAppData: LocalAppData?) {


    fun getId() = storeAppData.getId()

    fun loadIcon(imageView: ImageView) {

        localAppData?.icon?.let {
            imageView.setImageDrawable(it)
        } ?: storeAppData.loadLogo(imageView)
    }

    fun isInstalled() = localAppData != null
}