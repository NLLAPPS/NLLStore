package io.github.solrudev.simpleinstaller.impl.installer

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.solrudev.simpleinstaller.activityresult.ActionInstallPackageContract
import io.github.solrudev.simpleinstaller.apksource.ApkSource
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.data.ProgressData
import io.github.solrudev.simpleinstaller.data.SessionOptions
import io.github.solrudev.simpleinstaller.data.utils.makeIndeterminate
import io.github.solrudev.simpleinstaller.exceptions.SplitPackagesNotSupportedException
import io.github.solrudev.simpleinstaller.utils.extensions.clearTurnScreenOnSettings
import io.github.solrudev.simpleinstaller.utils.extensions.getParcelableCompat
import io.github.solrudev.simpleinstaller.utils.extensions.turnScreenOnWhenLocked
import io.github.solrudev.simpleinstaller.utils.getApplicationLabel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

private const val APK_URI_KEY = "PACKAGE_INSTALLER_APK_URI"

/**
 * Package installer implementation for 16 <= API < 21.
 */
internal object PackageInstallerApi16 : MinimalPackageInstaller {

	private val _progress = MutableSharedFlow<ProgressData>(
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)

	override val progress = _progress.asSharedFlow()

	override suspend fun installPackages(apkFiles: Array<out ApkSource>, options: SessionOptions): InstallResult {
		if (apkFiles.size > 1) {
			throw SplitPackagesNotSupportedException()
		}
		val apkFile = apkFiles.first()
		coroutineScope {
			val progressJob = launch {
				_progress.emitAll(apkFile.progress)
			}
			val apkUri = apkFile.getUri()
			progressJob.cancel()
			_progress.makeIndeterminate()
			launchConfirmation(options, apkUri, appLabelProducer = {
				getApplicationLabel(apkFile.file)
			})
		}
		return awaitInstallResultFromActivity()
	}

	private val installPackageContract = ActionInstallPackageContract()
	private var installResultDeferred: CompletableDeferred<InstallResult>? = null

	private suspend inline fun awaitInstallResultFromActivity(): InstallResult {
		val deferred = CompletableDeferred<InstallResult>(coroutineContext[Job])
		installResultDeferred = deferred
		return deferred.await()
	}

	private inline fun launchConfirmation(options: SessionOptions, apkUri: Uri, appLabelProducer: () -> CharSequence?) =
		launchInstallConfirmation<InstallLauncherActivity>(options, appLabelProducer) { intent ->
			intent.putExtra(APK_URI_KEY, apkUri)
		}

	internal class InstallLauncherActivity : AppCompatActivity() {

		private val installPackageLauncher = registerForActivityResult(installPackageContract) { success ->
			val result = if (success) InstallResult.Success else InstallResult.Failure()
			installResultDeferred?.complete(result)
		}

		override fun onCreate(savedInstanceState: Bundle?) {
			super.onCreate(savedInstanceState)
			isLauncherActivityCreated = true
			turnScreenOnWhenLocked()
			onInstallCompleted {
				isLauncherActivityCreated = false
				finish()
			}
			if (savedInstanceState != null) {
				return
			}
			launchInstallActivity()
		}

		override fun onDestroy() {
			super.onDestroy()
			clearTurnScreenOnSettings()
		}

		private fun launchInstallActivity() {
			val apkUri = intent.extras?.getParcelableCompat<Uri>(APK_URI_KEY)
			if (apkUri == null) {
				installResultDeferred?.completeExceptionally(
					IllegalStateException("InstallLauncherActivity: apkUri was null.")
				)
				return
			}
			installPackageLauncher.launch(apkUri)
		}
	}
}