package io.github.solrudev.simpleinstaller.impl.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.solrudev.simpleinstaller.SimpleInstaller.installerPackageName
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.utils.extensions.getParcelableExtraCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlin.coroutines.coroutineContext

private var installResultDeferred: CompletableDeferred<InstallResult>? = null

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@JvmSynthetic
internal suspend inline fun awaitInstallResultFromReceiver(): InstallResult {
	val deferred = CompletableDeferred<InstallResult>(coroutineContext[Job])
	installResultDeferred = deferred
	return deferred.await()
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class InstallationEventsReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action != ACTION) {
			return
		}
		val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
		val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
		if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
			val confirmationIntent = intent.getParcelableExtraCompat<Intent>(Intent.EXTRA_INTENT)
			if (confirmationIntent != null) {
				val wrapperIntent = Intent(context, PackageInstallerApi21.InstallLauncherActivity::class.java)
					.putExtra(Intent.EXTRA_INTENT, confirmationIntent)
					.putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				context.startActivity(wrapperIntent)
			} else {
				installResultDeferred?.completeExceptionally(
					IllegalArgumentException("confirmationIntent was null.")
				)
			}
			return
		}
		val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
		val otherPackageName = intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)
		val storagePath = intent.getStringExtra(PackageInstaller.EXTRA_STORAGE_PATH)
		installResultDeferred?.complete(
			InstallResult.fromStatusCode(status, message, otherPackageName, storagePath)
		)
	}

	internal companion object {

		@get:JvmSynthetic
		internal val ACTION by lazy { "$installerPackageName.INSTALLATION_STATUS" }
	}
}