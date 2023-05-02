package com.nll.store.debug

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.nll.store.R
import com.nll.store.utils.ApiLevel
import com.nll.store.utils.extGetThemeAttrColor
import io.karn.notify.Notify
import io.karn.notify.entities.Payload
import com.google.android.material.R as MaterialResources


object DebugNotification {
    private fun alertPayload(context: Context) = Payload.Alerts(
        channelKey = "debug-log-notification",
        lockScreenVisibility = NotificationCompat.VISIBILITY_PUBLIC,
        channelName = context.getString(R.string.debug_log),
        channelDescription = context.getString(R.string.debug_log),
        channelImportance = Notify.IMPORTANCE_LOW,
        showBadge = false

    )

    fun getDebugEnabledNotification(context: Context, startIntent: PendingIntent): NotificationCompat.Builder {
        val alertPayload = alertPayload(context)
        val builder = Notify.with(context)
            .meta {
                cancelOnClick = false
                sticky = true
                clickIntent = startIntent
                group = "debug"
            }
            .alerting(alertPayload.channelKey) {
                lockScreenVisibility = alertPayload.lockScreenVisibility
                channelName = alertPayload.channelName
                channelDescription = alertPayload.channelDescription
                channelImportance = alertPayload.channelImportance

            }
            .header {
                icon = R.drawable.notification_debug
                color = context.extGetThemeAttrColor(MaterialResources.attr.colorPrimary)
                showTimestamp = true
            }
            .content {
                title = context.getString(R.string.debug_log)

            }
            .asBuilder()
        if (ApiLevel.isSPlus()) {
            builder.foregroundServiceBehavior = Notification.FOREGROUND_SERVICE_IMMEDIATE
        }
        return builder
    }
}