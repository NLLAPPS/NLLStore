package io.github.solrudev.simpleinstaller.impl.installer

import android.app.PendingIntent
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.solrudev.simpleinstaller.PackageInstaller
import io.github.solrudev.simpleinstaller.R
import io.github.solrudev.simpleinstaller.SimpleInstaller.applicationContext
import io.github.solrudev.simpleinstaller.apksource.ApkSource
import io.github.solrudev.simpleinstaller.data.ConfirmationStrategy
import io.github.solrudev.simpleinstaller.data.InstallFailureCause
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.data.NotificationData
import io.github.solrudev.simpleinstaller.data.SessionOptions
import io.github.solrudev.simpleinstaller.exceptions.ApplicationContextNotSetException
import io.github.solrudev.simpleinstaller.utils.notificationManager
import io.github.solrudev.simpleinstaller.utils.pendingIntentCancelCurrentFlags
import io.github.solrudev.simpleinstaller.utils.showNotification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

private const val REQUEST_CODE = 2471
private const val NOTIFICATION_TAG = "PACKAGE_INSTALLER_NOTIFICATION"
private val notificationId = AtomicInteger(18475)
private val isInstallCompleted = MutableStateFlow(false)

@get:JvmSynthetic
internal var isLauncherActivityCreated = false

/**
 * A concrete implementation of [PackageInstaller] which delegates installation to [MinimalPackageInstaller].
 */
internal class PackageInstallerWrapper(
	private val packageInstaller: MinimalPackageInstaller
) : PackageInstaller {

	override var hasActiveSession = false
		private set

	override val progress = packageInstaller.progress

	override suspend fun installSplitPackage(vararg apkFiles: ApkSource, options: SessionOptions) =
		installPackages(apkFiles, options)

	override suspend fun installPackage(apkFile: ApkSource, options: SessionOptions) =
		installPackages(arrayOf(apkFile), options)

	override fun installSplitPackage(
		vararg apkFiles: ApkSource,
		options: SessionOptions,
		callback: PackageInstaller.Callback
	) = installPackages(apkFiles, options, callback)

	override fun installPackage(
		apkFile: ApkSource,
		options: SessionOptions,
		callback: PackageInstaller.Callback
	) = installPackages(arrayOf(apkFile), options, callback)

	override fun cancel() {
		installerScope.coroutineContext[Job]?.cancelChildren()
	}

	private val installerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
	private var currentApkSources: Array<out ApkSource> = emptyArray()

	private fun installPackages(
		apkFiles: Array<out ApkSource>,
		options: SessionOptions,
		callback: PackageInstaller.Callback
	) {
		installerScope.launch {
			val progressJob = progress
				.onEach(callback::onProgressChanged)
				.launchIn(this)
			try {
				when (val result = installPackages(apkFiles, options)) {
					is InstallResult.Success -> callback.onSuccess()
					is InstallResult.Failure -> callback.onFailure(result.cause)
				}
			} catch (_: CancellationException) {
				callback.onCanceled()
			} catch (t: Throwable) {
				callback.onException(t)
			} finally {
				progressJob.cancel()
			}
		}
	}

	private suspend fun installPackages(apkFiles: Array<out ApkSource>, options: SessionOptions): InstallResult {
		if (hasActiveSession) {
			error("Can't install while another install session is active.")
		}
		if (apkFiles.isEmpty()) {
			return InstallResult.Failure(InstallFailureCause.Generic("No APKs provided."))
		}
		hasActiveSession = true
		currentApkSources = apkFiles
		try {
			return packageInstaller.installPackages(apkFiles, options)
		} finally {
			onInstallCompleted()
		}
	}

	private fun onInstallCompleted() = try {
		currentApkSources.forEach { it.clearTempFiles() }
		notificationManager.cancel(NOTIFICATION_TAG, notificationId.get())
	} catch (_: ApplicationContextNotSetException) {
	} finally {
		if (isLauncherActivityCreated) {
			isInstallCompleted.value = true
		}
		currentApkSources = emptyArray()
		hasActiveSession = false
	}
}

@JvmSynthetic
internal inline fun ComponentActivity.onInstallCompleted(crossinline block: () -> Unit) = isInstallCompleted
	.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
	.filter { it }
	.onEach {
		isInstallCompleted.value = false
		block()
	}
	.launchIn(lifecycleScope)

@JvmSynthetic
internal inline fun <reified T : ComponentActivity> launchInstallConfirmation(
	options: SessionOptions,
	appLabelProducer: () -> CharSequence?,
	putExtra: (Intent) -> Unit
) {
	val intent = Intent(applicationContext, T::class.java).also(putExtra)
	when (options.confirmationStrategy) {
		ConfirmationStrategy.IMMEDIATE -> applicationContext.startActivity(
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		)
		ConfirmationStrategy.DEFERRED -> displayNotification(
			options.notificationData, intent, appLabelProducer
		)
	}
}

private inline fun displayNotification(
	notificationData: NotificationData,
	intent: Intent,
	appLabelProducer: () -> CharSequence?
) {
	val notification = NotificationData {
		title = notificationData.title.takeIf { it.isNotEmpty() } ?: applicationContext.getString(
			R.string.ssi_prompt_install_title
		)
		contentText = notificationData.contentText.takeIf { it.isNotEmpty() } ?: defaultMessage(appLabelProducer)
		icon = notificationData.icon
	}
	val fullScreenIntent = PendingIntent.getActivity(
		applicationContext,
		REQUEST_CODE,
		intent,
		pendingIntentCancelCurrentFlags
	)
	showNotification(fullScreenIntent, notificationId.incrementAndGet(), notification, NOTIFICATION_TAG)
}

private inline fun defaultMessage(appLabelProducer: () -> CharSequence?): String {
	val appLabel = appLabelProducer()
	return if (appLabel != null) {
		applicationContext.getString(R.string.ssi_prompt_install_message_with_label, appLabel)
	} else {
		applicationContext.getString(R.string.ssi_prompt_install_message)
	}
}