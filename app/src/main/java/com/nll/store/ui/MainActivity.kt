package com.nll.store.ui

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.launch
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.nll.store.databinding.ActivityMainBinding
import com.nll.store.viewmodels.MainViewModel
import io.github.solrudev.simpleinstaller.activityresult.InstallPermissionContract

import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

	/**
	 * TODO implement
	 * for progressBar:
	 * 			android:indeterminate="@{viewModel.isProgressIndeterminate}"
	 * 			android:max="@{viewModel.progressMax}"
	 * 			android:progress="@{viewModel.progress}"
	 *
	 * 	for installButton
	 * 				android:enabled="@{viewModel.isInstallEnabled}"
	 * 			android:onClick="@{() -> activity.onInstallButtonClick()}"
	 * 			android:text="@{viewModel.installButtonText}"
	 *
	 *
	 * 	for uninstall button
	 * 				android:onClick="@{() -> activity.onUninstallButtonClick()}"
	 * 			android:text="@{viewModel.uninstallButtonText}"
	 */

	private lateinit var binding: ActivityMainBinding
	private val viewModel: MainViewModel by viewModels()

	private val installPermissionLauncher = registerForActivityResult(InstallPermissionContract()) { isGranted ->
		if (isGranted) viewModel.enableInstallButton()
		requestNotificationPermission()
	}

	private val notificationPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted ->
		if (!isGranted) exitProcess(0)
	}

	private val pickApkLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
		uri?.let(viewModel::installPackage)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)
		if (savedInstanceState == null) {
			requestPermissions()
		}
	}

	fun onInstallButtonClick() {
		if (!viewModel.isInstalling) {
			try {
				pickApkLauncher.launch("application/vnd.android.package-archive")
			} catch (_: ActivityNotFoundException) {
			}
			return
		}
		viewModel.cancel()
	}

	fun onUninstallButtonClick() {
		val intent = Intent(this, UninstallActivity::class.java)
		startActivity(intent)
	}

	private fun requestPermissions() {
		if (packageManager.canRequestPackageInstalls()) {
			viewModel.enableInstallButton()
			requestNotificationPermission()
			return
		}
		installPermissionLauncher.launch()
	}

	private fun requestNotificationPermission() {
		notificationPermissionLauncher.launch(POST_NOTIFICATIONS)
	}
}