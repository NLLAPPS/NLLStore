package com.nll.store.utils

import android.content.Intent
import android.os.Parcelable
import java.io.Serializable

inline fun <reified T : Parcelable> Intent.extGetParcelableExtra(name: String?): T? {
    //Wait for https://issuetracker.google.com/issues/240585930
    //return extGetParcelableExtra(name, T::class.java)

    return getParcelableExtra(name)
}

@Suppress("DEPRECATION")
fun <T : Parcelable> Intent.extGetParcelableExtra(name: String?, clazz: Class<T>): T? {
    return if (ApiLevel.isTPlus()) {
        getParcelableExtra(name, clazz)
    } else {
        getParcelableExtra(name)
    }
}


inline fun <reified T : Serializable> Intent.extGetSerializableExtra(name: String?): T? {
    //Wait for https://issuetracker.google.com/issues/240585930
    //return extGetSerializableExtra(name, T::class.java)

    return getSerializableExtra(name) as? T?
}

@Suppress("DEPRECATION", "UNCHECKED_CAST")
fun <T : Serializable> Intent.extGetSerializableExtra(name: String?, clazz: Class<T>):  T? {
    return if (ApiLevel.isTPlus()) {
        getSerializableExtra(name, clazz)
    } else {
        getSerializableExtra(name) as? T?
    }
}