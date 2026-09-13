package com.jarvis.aegis.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.jarvis.aegis.data.AegisStore
import com.jarvis.aegis.ui.lock.AegisLockActivity

/** Package-transition-only enforcement. Window content retrieval is disabled in XML. */
class AegisAccessibilityService : AccessibilityService() {
    private var lastIntercepted: String? = null
    private var lastInterceptedAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val foregroundPackage = event.packageName?.toString() ?: return
        if (foregroundPackage == packageName) return
        val store = AegisStore(this)
        val session = store.activeSession() ?: return
        if (foregroundPackage !in session.policy.targetPackages || foregroundPackage in session.policy.essentialPackages) return
        if (store.isGranted(foregroundPackage)) return
        val now = System.currentTimeMillis()
        if (lastIntercepted == foregroundPackage && now - lastInterceptedAt < 1_500) return
        lastIntercepted = foregroundPackage
        lastInterceptedAt = now
        startActivity(Intent(this, AegisLockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            putExtra(AegisLockActivity.EXTRA_TARGET, foregroundPackage)
        })
    }

    override fun onInterrupt() = Unit
}
