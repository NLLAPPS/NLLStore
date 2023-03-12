package io.github.solrudev.simpleinstaller.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import io.github.solrudev.simpleinstaller.R
import io.github.solrudev.simpleinstaller.SimpleInstaller.applicationContext
import io.github.solrudev.simpleinstaller.data.NotificationData

@get:JvmSynthetic
internal val notificationManager
	get() = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

@get:JvmSynthetic
internal val pendingIntentUpdateCurrentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
	PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
} else {
	PendingIntent.FLAG_UPDATE_CURRENT
}

@get:JvmSynthetic
internal val pendingIntentCancelCurrentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
	PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
} else {
	PendingIntent.FLAG_CANCEL_CURRENT
}

@JvmSynthetic
internal fun showNotification(
	intent: PendingIntent,
	notificationId: Int,
	notificationData: NotificationData,
	notificationTag: String
) {
	val channelId = applicationContext.getString(R.string.ssi_notification_channel_id)
	val notification = NotificationCompat.Builder(applicationContext, channelId).apply {
		setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
		setContentTitle(notificationData.title)
		setContentText(notificationData.contentText)
		if (notificationData.contentText.length > 28) {
			setStyle(NotificationCompat.BigTextStyle().bigText(notificationData.contentText))
		}
		setContentIntent(intent)
		priority = NotificationCompat.PRIORITY_MAX
		setDefaults(NotificationCompat.DEFAULT_ALL)
		setSmallIcon(notificationData.icon)
		setOngoing(true)
		setFullScreenIntent(intent, true)
		setAutoCancel(true)
	}.build()
	notificationManager.notify(notificationTag, notificationId, notification)
}