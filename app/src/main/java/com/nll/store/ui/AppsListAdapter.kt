package com.nll.store.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.nll.store.databinding.RowAppItemBinding
import com.nll.store.model.AppData
import com.nll.store.model.LocalAppData
import com.nll.store.model.StoreAppData


class AppsListAdapter(private val callback: CallBack) : ListAdapter<AppData, AppListViewHolder>(DiffCallback) {
    private val logTag = "AppsListAdapter"
    override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
        holder.bind(getItem(position), callback, position)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppListViewHolder {
        return AppListViewHolder(RowAppItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    interface CallBack {
        fun onCardClick(data: AppData, position: Int)
        fun onInstallClick(storeAppData: StoreAppData, position: Int)
        fun onOpenClick(localAppData: LocalAppData, position: Int)
    }

    object DiffCallback : DiffUtil.ItemCallback<AppData>() {
        override fun areItemsTheSame(oldItem: AppData, newItem: AppData): Boolean {
            return oldItem.getId() == newItem.getId()
        }

        override fun areContentsTheSame(oldItem: AppData, newItem: AppData): Boolean {
            return oldItem == newItem
        }


    }
}