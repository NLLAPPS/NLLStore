package com.nll.store.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nll.store.AppData
import io.github.solrudev.simpleinstaller.PackageUninstaller
import io.github.solrudev.simpleinstaller.data.ConfirmationStrategy
import io.github.solrudev.simpleinstaller.uninstallPackage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UninstallViewModel : ViewModel() {

	private val _appsList = MutableStateFlow(listOf<AppData>())
	val appsList = _appsList.asStateFlow()
	val isUninstalling get() = PackageUninstaller.hasActiveSession

	fun uninstallPackage(appData: AppData) {
		viewModelScope.launch {
			val result = PackageUninstaller.uninstallPackage(appData.packageName) {
				confirmationStrategy = ConfirmationStrategy.IMMEDIATE
			}
			if (result) {
				removeApp(appData)
			}
		}
	}

	fun setAppsList(appsList: List<AppData>) {
		_appsList.value = appsList
	}

	private fun removeApp(appData: AppData) {
		val newList = _appsList.value.toMutableList()
		newList.remove(appData)
		_appsList.value = newList
	}
}