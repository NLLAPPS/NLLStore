package com.nll.store.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import com.nll.store.R
import com.nll.store.ui.AppListActivity
import com.nll.store.utils.extGetThemeAttrColor
import io.karn.notify.Notify
import io.karn.notify.entities.Payload

object UpdateNotification {
    private fun alertPayload(context: Context) = Payload.Alerts(
        channelKey = "update-notification",
        lockScreenVisibility = NotificationCompat.VISIBILITY_PUBLIC,
        channelName = context.getString(R.string.update),
        channelDescription = context.getString(R.string.update),
        channelImportance = Notify.IMPORTANCE_NORMAL,
        showBadge = false

    )

    fun postUpdateNotification(context: Context) {
        val notificationColor = ContextThemeWrapper(context.applicationContext, R.style.AppTheme).extGetThemeAttrColor(com.google.android.material.R.attr.colorPrimary)
        val startIntent = Intent(context, AppListActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val startPendingIntent = PendingIntent.getActivity(context, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alertPayload = alertPayload(context)
        Notify.with(context)
            .meta {
                cancelOnClick = true
                sticky = false
                clickIntent = startPendingIntent
                group = "update"
            }
            .alerting(alertPayload.channelKey) {
                lockScreenVisibility = alertPayload.lockScreenVisibility
                channelName = alertPayload.channelName
                channelDescription = alertPayload.channelDescription
                channelImportance = alertPayload.channelImportance

            }
            .header {
                icon = R.drawable.ic_update_found
                color = notificationColor
                colorized = true
                showTimestamp = true
            }
            .content {
                title = context.getString(R.string.update_found)

            }
            .show("update".hashCode())


    }
}