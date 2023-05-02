package com.nll.store.model

sealed class AppInstallState {
    object NotInstalled: AppInstallState()
    data class Installed(val localAppData: LocalAppData): AppInstallState()

}