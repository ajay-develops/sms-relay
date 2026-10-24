package com.saulredemptionservicesllc.smsrelay.sms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.Notification
import com.saulredemptionservicesllc.smsrelay.R
import com.saulredemptionservicesllc.smsrelay.data.LogEntry
import com.saulredemptionservicesllc.smsrelay.ui.MainActivity

object Notifier {
    private const val CHANNEL = "failures"

    fun forwardFailed(context: Context, entry: LogEntry) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= 33
        ) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Failed forwards", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Alerts when a message could not be forwarded" },
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_relay)
            .setContentTitle("Couldn't forward a message")
            .setContentText("To ${entry.toName ?: entry.to}: ${entry.error ?: "unknown error"}")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(entry.id.hashCode(), n)
    }
}
