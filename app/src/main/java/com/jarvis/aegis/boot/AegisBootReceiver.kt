package com.jarvis.aegis.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jarvis.aegis.data.AegisStore
import com.jarvis.aegis.notification.AegisNotifications
import com.jarvis.aegis.recovery.WatchdogManager

/** Validates only; it never launches an activity during boot. */
class AegisBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED && intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val notifications = AegisNotifications(context).also { it.createChannels() }
        if (WatchdogManager(context).isFailOpen()) {
            notifications.showFailOpen("Crash recovery is active. Restrictions remain paused.")
            return
        }
        AegisStore(context).activeSession()?.let(notifications::showActiveSession)
            ?: notifications.cancelSession()
    }
}
