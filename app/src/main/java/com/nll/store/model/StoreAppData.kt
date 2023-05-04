package com.nll.store.model

import android.widget.ImageView
import coil.imageLoader
import coil.request.ImageRequest
import com.nll.store.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoreAppData(
    @SerialName("isNLLStoreApp") val isNLLStoreApp: Boolean,
    @SerialName("name") val name: String,
    @SerialName("packageName") val packageName: String,
    @SerialName("version") val version: Long,
    @SerialName("downloadUrl") val downloadUrl: String,
    @SerialName("autoUpdate") val autoUpdate: Boolean,
    @SerialName("logoUrl") val logoUrl: String,
    @SerialName("description") val description: String,
    @SerialName("versionNotes") val versionNotes: String,
    @SerialName("website") val website: String
){
    fun getId() = packageName.hashCode()

    fun loadLogo(imageView: ImageView){
        val request = ImageRequest.Builder(imageView.context)
            .placeholder(R.drawable.ic_place_holder_24dp)
            .data(logoUrl)
            .crossfade(false)
            .target(imageView)
            .build()
        imageView.context.imageLoader.enqueue(request)
    }
}


