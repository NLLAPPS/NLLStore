package io.github.solrudev.simpleinstaller.impl.installer

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionCallback
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import io.github.solrudev.simpleinstaller.SimpleInstaller.packageManager
import io.github.solrudev.simpleinstaller.apksource.ApkSource
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.data.ProgressData
import io.github.solrudev.simpleinstaller.data.SessionOptions
import io.github.solrudev.simpleinstaller.data.utils.tryEmit
import io.github.solrudev.simpleinstaller.utils.*
import io.github.solrudev.simpleinstaller.utils.extensions.clearTurnScreenOnSettings
import io.github.solrudev.simpleinstaller.utils.extensions.getParcelableCompat
import io.github.solrudev.simpleinstaller.utils.extensions.turnScreenOnWhenLocked
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

private const val REQUEST_CODE = 6541

/**
 * Package installer implementation for API >= 21.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal object PackageInstallerApi21 : MinimalPackageInstaller {

	private val _progress = MutableSharedFlow<ProgressData>(
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)

	override val progress = _progress.asSharedFlow()

	override suspend fun installPackages(
		apkFiles: Array<out ApkSource>,
		options: SessionOptions
	) = withContext(Dispatchers.IO) main@{
		var sessionId = -1
		var sessionCallback: SessionCallback? = null
		try {
			val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				sessionParams.setInstallReason(PackageManager.INSTALL_REASON_USER)

			}
			sessionId = packageInstaller.createSession(sessionParams)
			sessionCallback = packageInstaller.createAndRegisterSessionCallback(sessionId)
			packageInstaller.openSession(sessionId).use { session ->
				session.copyApksFrom(apkFiles)
				launchConfirmation(options, sessionId, appLabelProducer = {
					val file = File(buildSessionDir(sessionId), session.names.first())
					getApplicationLabel(file)
				})
			}
			if (!awaitIsSessionAlive()) {
				abandonSession(sessionId)
				return@main InstallResult.Failure()
			}
			return@main awaitInstallResultFromReceiver()
		} catch (t: Throwable) {
			abandonSession(sessionId)
			throw t
		} finally {
			onInstallCompleted(sessionCallback)
		}
	}

	private var isSessionAliveDeferred: CompletableDeferred<Boolean>? = null

	private val packageInstaller
		get() = packageManager.packageInstaller

	private fun packageInstallerSessionCallback(currentSessionId: Int) = object : SessionCallback() {
		override fun onCreated(sessionId: Int) {}
		override fun onBadgingChanged(sessionId: Int) {}
		override fun onActiveChanged(sessionId: Int, active: Boolean) {}
		override fun onFinished(sessionId: Int, success: Boolean) {}

		override fun onProgressChanged(sessionId: Int, progress: Float) {
			if (sessionId == currentSessionId) {
				_progress.tryEmit((progress * 100).toInt(), 100)
			}
		}
	}

	private suspend inline fun PackageInstaller.createAndRegisterSessionCallback(sessionId: Int): SessionCallback {
		val callback = packageInstallerSessionCallback(sessionId)
		withContext(Dispatchers.Main) {
			registerSessionCallback(callback)
		}
		return callback
	}

	private suspend inline fun PackageInstaller.Session.copyApksFrom(apkFiles: Array<out ApkSource>) {
		val totalLength = apkFiles.sumOf { it.length }
		var transferredBytes = 0L
		apkFiles.forEachIndexed { index, apkFile ->
			// though `copyTo` closes streams, we need to ensure that if opening sessionStream fails, apkStream is closed
			apkFile.openInputStream().use { apkStream ->
				requireNotNull(apkStream) { "APK $index InputStream was null." }
				val sessionStream = openWrite("temp$index.apk", 0, apkFile.length)
				apkStream.copyTo(sessionStream, totalLength, transferredBytes, onProgressChanged = { progress, max ->
					setStagingProgress(progress.toFloat() / max)
				})
			}
			transferredBytes += apkFile.length
		}
	}

	private fun abandonSession(sessionId: Int) = try {
		packageInstaller.abandonSession(sessionId)
	} catch (_: Throwable) {
	}

	private fun onInstallCompleted(sessionCallback: SessionCallback?) {
		sessionCallback?.let(packageInstaller::unregisterSessionCallback)
	}

	private suspend inline fun awaitIsSessionAlive(): Boolean {
		val deferred = CompletableDeferred<Boolean>(coroutineContext[Job])
		isSessionAliveDeferred = deferred
		return deferred.await()
	}

	private inline fun launchConfirmation(
		options: SessionOptions,
		sessionId: Int,
		appLabelProducer: () -> CharSequence?
	) = launchInstallConfirmation<InstallLauncherActivity>(options, appLabelProducer) { intent ->
		intent.putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
	}

	internal class InstallLauncherActivity : AppCompatActivity() {

		private val sessionId by lazy {
			val sessionId = intent.extras?.getInt(PackageInstaller.EXTRA_SESSION_ID)
			if (sessionId == null) {
				isSessionAliveDeferred?.completeExceptionally(
					IllegalStateException("InstallLauncherActivity: sessionId was null.")
				)
			}
			sessionId ?: -1
		}

		private val confirmationLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
				val sessionInfo = packageInstaller.getSessionInfo(sessionId)
				// Hacky workaround: progress not going higher than 0.8 means session is dead. This is needed to resume
				// the coroutine with failure on reasons which are not handled in InstallationEventsReceiver.
				// For example, "There was a problem parsing the package" error falls under that.
				val isSessionAlive = sessionInfo != null && sessionInfo.progress >= 0.81
				isSessionAliveDeferred?.complete(isSessionAlive)
				if (isSessionAlive) {
					finish()
				}
			}

		override fun onCreate(savedInstanceState: Bundle?) {
			super.onCreate(savedInstanceState)
			turnScreenOnWhenLocked()
			onInstallCompleted {
				isLauncherActivityCreated = false
				finish()
			}
			if (savedInstanceState != null) {
				return
			}
			if (!isLauncherActivityCreated) {
				isLauncherActivityCreated = true
				commitSession()
			}
			launchInstallActivity()
		}

		override fun onNewIntent(intent: Intent?) {
			super.onNewIntent(intent)
			startActivity(intent)
			finish()
		}

		override fun onDestroy() {
			super.onDestroy()
			clearTurnScreenOnSettings()
		}

		private fun launchInstallActivity() {
			intent.extras
				?.getParcelableCompat<Intent>(Intent.EXTRA_INTENT)
				?.let(confirmationLauncher::launch)
		}

		private fun commitSession() {
			val receiverIntent = Intent(applicationContext, InstallationEventsReceiver::class.java).apply {
				action = InstallationEventsReceiver.ACTION
			}
			val receiverPendingIntent = PendingIntent.getBroadcast(
				applicationContext,
				REQUEST_CODE,
				receiverIntent,
				pendingIntentUpdateCurrentFlags
			)
			val statusReceiver = receiverPendingIntent.intentSender
			packageInstaller
				.openSession(sessionId)
				.commit(statusReceiver)
		}
	}
}