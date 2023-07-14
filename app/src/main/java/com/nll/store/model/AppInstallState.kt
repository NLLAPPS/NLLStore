package com.nll.store.model

sealed class AppInstallState {
    data object NotInstalled: AppInstallState()
    data class Installed(val localAppData: LocalAppData): AppInstallState()

}