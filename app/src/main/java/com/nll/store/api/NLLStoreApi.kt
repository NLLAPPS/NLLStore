package com.nll.store.api

import com.nll.store.model.StoreAppData
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType

class NLLStoreApi() {
    private val apiUrl = "https://nllapps.com/store/api/"
    private val transport = HttpTransport(ApiClient.createHttpClient(apiUrl))
    suspend fun getStoreAppList(): List<StoreAppData> = transport.perform {
        it.get {
            url(path = "apps.json")
            contentType(ContentType.Any)
        }
    }
}