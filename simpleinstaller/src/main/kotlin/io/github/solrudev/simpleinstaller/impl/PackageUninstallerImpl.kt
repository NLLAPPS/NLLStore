package io.github.solrudev.simpleinstaller.impl

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.solrudev.simpleinstaller.PackageUninstaller
import io.github.solrudev.simpleinstaller.R
import io.github.solrudev.simpleinstaller.SimpleInstaller.applicationContext
import io.github.solrudev.simpleinstaller.activityresult.UninstallPackageContract
import io.github.solrudev.simpleinstaller.data.ConfirmationStrategy
import io.github.solrudev.simpleinstaller.data.NotificationData
import io.github.solrudev.simpleinstaller.data.SessionOptions
import io.github.solrudev.simpleinstaller.exceptions.ApplicationContextNotSetException
import io.github.solrudev.simpleinstaller.utils.extensions.clearTurnScreenOnSettings
import io.github.solrudev.simpleinstaller.utils.extensions.turnScreenOnWhenLocked
import io.github.solrudev.simpleinstaller.utils.getApplicationLabel
import io.github.solrudev.simpleinstaller.utils.notificationManager
import io.github.solrudev.simpleinstaller.utils.pendingIntentUpdateCurrentFlags
import io.github.solrudev.simpleinstaller.utils.showNotification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.coroutines.coroutineContext

private const val PACKAGE_NAME_KEY = "PACKAGE_UNINSTALLER_PACKAGE_NAME"
private const val NOTIFICATION_TAG = "PACKAGE_UNINSTALLER_NOTIFICATION"
private const val REQUEST_CODE = 4127

/**
 * A concrete implementation of [PackageUninstaller].
 */
internal object PackageUninstallerImpl : PackageUninstaller {

	override var hasActiveSession = false
		private set

	override suspend fun uninstallPackage(packageName: String, options: SessionOptions): Boolean {
		if (hasActiveSession) {
			error("Can't uninstall while another uninstall session is active.")
		}
		hasActiveSession = true
		try {
			launchConfirmation(options, packageName)
			return awaitUninstallResultFromActivity()
		} finally {
			onUninstallCompleted()
		}
	}

	override fun uninstallPackage(packageName: String, options: SessionOptions, callback: PackageUninstaller.Callback) {
		uninstallerScope.launch {
			try {
				val result = uninstallPackage(packageName, options)
				callback.onFinished(result)
			} catch (_: CancellationException) {
				callback.onCanceled()
			} catch (t: Throwable) {
				callback.onException(t)
			}
		}
	}

	override fun cancel() {
		uninstallerScope.coroutineContext[Job]?.cancelChildren()
	}

	private val uninstallPackageContract = UninstallPackageContract()
	private val uninstallerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
	private val isUninstallCompleted = MutableStateFlow(false)
	private val notificationId = AtomicInteger(34187)
	private var isLauncherActivityCreated = false
	private var uninstallResultDeferred: CompletableDeferred<Boolean>? = null

	private suspend inline fun awaitUninstallResultFromActivity(): Boolean {
		val deferred = CompletableDeferred<Boolean>(coroutineContext[Job])
		uninstallResultDeferred = deferred
		return deferred.await()
	}

	private fun launchConfirmation(options: SessionOptions, packageName: String) {
		val intent = Intent(applicationContext, UninstallLauncherActivity::class.java)
			.putExtra(PACKAGE_NAME_KEY, packageName)
		when (options.confirmationStrategy) {
			ConfirmationStrategy.IMMEDIATE -> applicationContext.startActivity(
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			)
			ConfirmationStrategy.DEFERRED -> displayNotification(
				options.notificationData, intent, packageName
			)
		}
	}

	private fun displayNotification(notificationData: NotificationData, intent: Intent, packageName: String) {
		val message by lazy(LazyThreadSafetyMode.NONE) {
			val appLabel = getApplicationLabel(packageName)
			if (appLabel != null) {
				applicationContext.getString(R.string.ssi_prompt_uninstall_message_with_label, appLabel)
			} else {
				applicationContext.getString(R.string.ssi_prompt_uninstall_message)
			}
		}
		val notification = NotificationData {
			title = notificationData.title.takeIf { it.isNotEmpty() } ?: applicationContext.getString(
				R.string.ssi_prompt_uninstall_title
			)
			contentText = notificationData.contentText.takeIf { it.isNotEmpty() } ?: message
			icon = notificationData.icon
		}
		val fullScreenIntent = PendingIntent.getActivity(
			applicationContext,
			REQUEST_CODE,
			intent,
			pendingIntentUpdateCurrentFlags
		)
		showNotification(fullScreenIntent, notificationId.incrementAndGet(), notification, NOTIFICATION_TAG)
	}

	private fun onUninstallCompleted() = try {
		notificationManager.cancel(NOTIFICATION_TAG, notificationId.get())
	} catch (_: ApplicationContextNotSetException) {
	} finally {
		if (isLauncherActivityCreated) {
			isUninstallCompleted.value = true
		}
		hasActiveSession = false
	}

	private inline fun ComponentActivity.onUninstallCompleted(crossinline block: () -> Unit) = isUninstallCompleted
		.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
		.filter { it }
		.onEach {
			isUninstallCompleted.value = false
			block()
		}
		.launchIn(lifecycleScope)

	internal class UninstallLauncherActivity : AppCompatActivity() {

		private val uninstallLauncher = registerForActivityResult(uninstallPackageContract) { result ->
			uninstallResultDeferred?.complete(result)
		}

		override fun onCreate(savedInstanceState: Bundle?) {
			super.onCreate(savedInstanceState)
			isLauncherActivityCreated = true
			turnScreenOnWhenLocked()
			onUninstallCompleted {
				isLauncherActivityCreated = false
				finish()
			}
			if (savedInstanceState != null) {
				return
			}
			launchUninstall()
		}

		override fun onDestroy() {
			super.onDestroy()
			clearTurnScreenOnSettings()
		}

		private fun launchUninstall() {
			val packageName = intent.extras?.getString(PACKAGE_NAME_KEY)
			if (packageName == null) {
				uninstallResultDeferred?.completeExceptionally(
					IllegalStateException("UninstallLauncherActivity: packageName was null.")
				)
				return
			}
			uninstallLauncher.launch(packageName)
		}
	}
}