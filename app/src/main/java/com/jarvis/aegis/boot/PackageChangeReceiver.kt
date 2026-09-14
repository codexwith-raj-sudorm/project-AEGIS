package com.jarvis.aegis.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jarvis.aegis.data.AegisStore
import com.jarvis.aegis.notification.AegisNotifications

/** Warns about changed application surfaces; it never silently expands the target list. */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in setOf(Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED)) return
        val changed = intent.data?.schemeSpecificPart ?: return
        if (changed == context.packageName || AegisStore(context).activeSession() == null) return
        AegisNotifications(context).apply {
            createChannels()
            showSecurityNotice(
                title = "Application surface changed",
                message = "$changed was installed or updated during an active session. Review possible alternate access paths.",
            )
        }
    }
}
