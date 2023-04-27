package com.nll.store.model

import android.net.Uri
import io.github.solrudev.simpleinstaller.data.InstallResult

sealed class InstallState {
    object Idle: InstallState()
    class Installing(uri: Uri): InstallState()
    class Uninstalling(val data: LocalAppData): InstallState()
    class Completed(uri: Uri, installResult: InstallResult): InstallState()

}