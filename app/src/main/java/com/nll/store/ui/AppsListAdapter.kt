package com.nll.store.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nll.store.databinding.RowAppItemBinding
import com.nll.store.log.CLog
import com.nll.store.model.AppData


class AppsListAdapter(private val callback: CallBack) : ListAdapter<AppData, AppsListAdapter.AppViewHolder>(DiffCallback) {
    private val logTag = "AppsListAdapter"
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position), callback, position)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return AppViewHolder(RowAppItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    fun interface CallBack {
        fun onAppDataClick(data: AppData, position: Int)
    }

    class AppViewHolder(private val binding: RowAppItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private val logTag = "AppViewHolder"
        fun bind(data: AppData, callback: CallBack, position: Int) {
            if (CLog.isDebug()) {
                CLog.log(logTag, "bind() -> data: $data")
            }
            binding.root.setOnClickListener {
                callback.onAppDataClick(data, position)
            }
            data.loadIcon(binding.appIcon)
            binding.appName.text = data.storeAppData.name
            binding.appDescription.text = data.storeAppData.description


        }

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