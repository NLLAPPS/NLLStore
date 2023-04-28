package com.nll.store.ui

import androidx.recyclerview.widget.RecyclerView
import com.nll.store.R
import com.nll.store.databinding.RowAppItemBinding
import com.nll.store.log.CLog
import com.nll.store.model.AppData
import com.nll.store.model.AppInstallState

class AppListViewHolder(private val binding: RowAppItemBinding) : RecyclerView.ViewHolder(binding.root) {
    private val logTag = "AppListViewHolder"
    fun bind(data: AppData, callback: AppsListAdapter.CallBack, position: Int) {
        if (CLog.isDebug()) {
            CLog.log(logTag, "bind() -> data: $data")
        }
        binding.root.setOnClickListener {
            callback.onCardClick(data, position)
        }
        binding.appActionButton.setOnClickListener {
            when (data.appInstallState) {
                is AppInstallState.Installed -> callback.onOpenClick(data.appInstallState.localAppData, position)
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