package com.nll.store.model

import android.net.Uri
import io.github.solrudev.simpleinstaller.data.InstallResult

sealed class InstallSessionState {
    object Idle: InstallSessionState()
    class Installing(uri: Uri): InstallSessionState()
    class Uninstalling(val data: LocalAppData): InstallSessionState()
    class Completed(uri: Uri, installResult: InstallResult): InstallSessionState()

}