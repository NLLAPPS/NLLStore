package io.github.solrudev.simpleinstaller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentCallbacks
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import io.github.solrudev.simpleinstaller.exceptions.SimpleInstallerReinitializeException
import io.github.solrudev.simpleinstaller.utils.notificationManager
import io.github.solrudev.simpleinstaller.utils.requireContextNotNull

/**
 * Easy to use Android package installer wrapper leveraging Kotlin coroutines (API 16+).
 */
internal object SimpleInstaller {

	@get:JvmSynthetic
	internal val applicationContext: Context
		get() = requireContextNotNull(_applicationContext)

	@get:JvmSynthetic
	internal val installerPackageName: String
		get() = applicationContext.packageName

	@get:JvmSynthetic
	internal val packageManager: PackageManager
		get() = applicationContext.packageManager

	private var _applicationContext: Context? = null

	private val configurationChangesCallback = object : ComponentCallbacks {
		override fun onConfigurationChanged(newConfig: Configuration) = createNotificationChannel()
		override fun onLowMemory() {}
	}

	private val lock = Any()

	@JvmSynthetic
	internal fun initialize(context: Context) = synchronized(lock) {
		if (_applicationContext != null) {
			throw SimpleInstallerReinitializeException()
		}
		_applicationContext = context.applicationContext
		_applicationContext?.registerComponentCallbacks(configurationChangesCallback)
		createNotificationChannel()
	}

	private fun createNotificationChannel() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			return
		}
		val channelIdString = applicationContext.getString(R.string.ssi_notification_channel_id)
		val channelName = applicationContext.getString(R.string.ssi_notification_channel_name)
		val channelDescription = applicationContext.getString(R.string.ssi_notification_channel_description)
		val importance = NotificationManager.IMPORTANCE_HIGH
		val channel = NotificationChannel(channelIdString, channelName, importance).apply {
			description = channelDescription
		}
		notificationManager.createNotificationChannel(channel)
	}
}