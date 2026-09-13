package com.jarvis.aegis.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jarvis.aegis.MainActivity
import com.jarvis.aegis.session.FocusSession

class AegisNotifications(private val context: Context) {
    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannels(
                listOf(
                    NotificationChannel(SESSION_CHANNEL, "Active focus sessions", NotificationManager.IMPORTANCE_LOW),
                    NotificationChannel(SAFETY_CHANNEL, "AEGIS safety status", NotificationManager.IMPORTANCE_HIGH),
                ),
            )
        }
    }

    fun showActiveSession(session: FocusSession) {
        val pendingIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        notify(
            SESSION_ID,
            NotificationCompat.Builder(context, SESSION_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("AEGIS ${session.policy.mode.name} session active")
                .setContentText("Ends ${session.expiresAt}. ${session.policy.targetPackages.size} targets protected.")
                .setContentIntent(pendingIntent).setOngoing(true).setOnlyAlertOnce(true).build(),
        )
    }

    fun showFailOpen(reason: String) = notify(
        SAFETY_ID,
        NotificationCompat.Builder(context, SAFETY_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("AEGIS enforcement paused")
            .setContentText(reason).setAutoCancel(true).build(),
    )

    fun cancelSession() = NotificationManagerCompat.from(context).cancel(SESSION_ID)

    private fun notify(id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }

    companion object {
        const val SESSION_CHANNEL = "aegis_active_session"
        const val SAFETY_CHANNEL = "aegis_safety"
        private const val SESSION_ID = 1001
        private const val SAFETY_ID = 1002
    }
}
