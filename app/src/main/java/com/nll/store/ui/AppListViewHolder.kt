package com.nll.store.ui

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nll.store.R
import com.nll.store.databinding.RowAppItemBinding
import com.nll.store.log.CLog
import com.nll.store.model.AppData
import com.nll.store.model.AppInstallState
import com.nll.store.utils.extGetThemeAttrColor

class AppListViewHolder(private val binding: RowAppItemBinding) : RecyclerView.ViewHolder(binding.root) {
    private val logTag = "AppListViewHolder"

    fun bind(data: AppData, callback: AppsListAdapter.CallBack, position: Int) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "bind() -> data: $data")
        }

        binding.appInfo.setOnClickListener {
            callback.onCardClick(data, position)
        }

        //Do not allow tapping on action button if this is actual NLL Store app and does not need updating
        val allowTapping = data.canBeUpdated() || !data.isSelf()
        binding.appActionButton.isVisible = allowTapping

        binding.appActionButton.backgroundTintList = ColorStateList.valueOf(
            binding.root.context.extGetThemeAttrColor(
                    if (data.canBeUpdated()) {
                        R.attr.customColorSuccess
                    } else {
                        androidx.appcompat.R.attr.colorPrimary
                    }
            )
        )

        binding.appActionButton.setOnClickListener {

            when (data.appInstallState) {
                is AppInstallState.Installed -> {
                    if (data.canBeUpdated()) {
                        callback.onUpdateClick(data.storeAppData, data.appInstallState.localAppData, position)
                    } else {
                        callback.onOpenClick(data.appInstallState.localAppData, position)
                    }
                }

                AppInstallState.NotInstalled -> callback.onInstallClick(data.storeAppData, position)
            }

        }

        data.loadIcon(binding.appIcon)
        binding.appName.text = data.storeAppData.name
        binding.appDescription.text = data.storeAppData.description
        binding.appActionButton.text = when (data.appInstallState) {

            is AppInstallState.Installed -> {
                if (data.canBeUpdated()) {
                    binding.root.context.getString(R.string.update)
                } else {
                    binding.root.context.getString(R.string.open)
                }
            }

            AppInstallState.NotInstalled -> binding.root.context.getString(R.string.install)
        }


    }

}