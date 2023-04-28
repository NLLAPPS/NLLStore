package com.nll.store.api

import com.nll.store.model.StoreAppData
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType

class NLLStoreApi() {
    private val transport = HttpTransport(Client.createHttpClient())
    suspend fun getStoreAppList(): List<StoreAppData> = transport.perform {
        it.get {
            url(path = "apps.json")
            contentType(ContentType.Any)
        }
    }
}