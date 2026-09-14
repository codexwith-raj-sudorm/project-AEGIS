package com.jarvis.aegis.security

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import com.jarvis.aegis.accessibility.AegisAccessibilityService
import com.jarvis.aegis.recovery.WatchdogManager
import com.jarvis.aegis.session.SessionMode
import com.jarvis.aegis.session.SessionPolicy
import com.jarvis.aegis.session.rules

enum class CheckSeverity { PASS, WARNING, BLOCKER }

data class PreflightCheck(val id: String, val label: String, val severity: CheckSeverity, val detail: String)
data class PreflightReport(val checks: List<PreflightCheck>) {
    val canActivate: Boolean get() = checks.none { it.severity == CheckSeverity.BLOCKER }
}

class EnforcementPreflight(private val context: Context) {
    fun run(policy: SessionPolicy): PreflightReport = PreflightReport(
        listOf(
            accessibility(policy),
            authentication(policy),
            targets(policy),
            essentials(policy),
            encryptedStorage(),
            watchdog(policy),
            notifications(),
        ),
    )

    private fun accessibility(policy: SessionPolicy): PreflightCheck {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        val expected = ComponentName(context, AegisAccessibilityService::class.java)
        val enabled = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { ComponentName.unflattenFromString(it.resolveInfo.serviceInfo.run { "$packageName/$name" }) == expected }
        val required = policy.mode != SessionMode.STANDARD
        return when {
            enabled -> pass("accessibility", "Accessibility interception", "AEGIS target detection is enabled.")
            required -> blocker("accessibility", "Accessibility interception", "Enable AEGIS Accessibility before activating Strict or Hard mode.")
            else -> warning("accessibility", "Accessibility interception", "Targets cannot be intercepted until AEGIS Accessibility is enabled.")
        }
    }

    private fun authentication(policy: SessionPolicy): PreflightCheck {
        if (!policy.mode.rules.requireDeviceAuthentication) return pass("authentication", "Device authentication", "Not required for Standard mode.")
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return if (BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
            pass("authentication", "Device authentication", "Biometric or device credential is available.")
        } else blocker("authentication", "Device authentication", "Configure a device PIN, pattern, password, or strong biometric first.")
    }

    private fun targets(policy: SessionPolicy): PreflightCheck {
        val missing = policy.targetPackages.filterNot(::isInstalled)
        return if (missing.isEmpty()) pass("targets", "Target packages", "${policy.targetPackages.size} selected target(s) are installed.")
        else blocker("targets", "Target packages", "Missing target packages: ${missing.joinToString()}")
    }

    private fun essentials(policy: SessionPolicy): PreflightCheck {
        val overlap = policy.targetPackages.intersect(policy.essentialPackages)
        val required = EssentialAccessResolver(context).requiredPackages()
        val missing = required - policy.essentialPackages
        return if (overlap.isEmpty() && missing.isEmpty()) {
            pass("essentials", "Essential access", "${required.size} required system and recovery packages are exempt.")
        } else blocker(
            "essentials", "Essential access",
            "Required exemptions missing: ${missing.joinToString().ifBlank { "none" }}; overlap: ${overlap.joinToString().ifBlank { "none" }}",
        )
    }

    private fun encryptedStorage(): PreflightCheck = runCatching {
        val cipher = KeystoreCipher()
        val probe = "aegis-preflight-${System.nanoTime()}"
        check(cipher.decrypt(cipher.encrypt(probe)) == probe)
        pass("storage", "Encrypted recovery state", "Android Keystore encryption read/write test passed.")
    }.getOrElse { blocker("storage", "Encrypted recovery state", "Keystore self-test failed: ${it.javaClass.simpleName}") }

    private fun watchdog(policy: SessionPolicy): PreflightCheck {
        val failOpen = WatchdogManager(context).isFailOpen()
        return when {
            !failOpen -> pass("watchdog", "Safety watchdog", "Watchdog is healthy.")
            policy.mode == SessionMode.STANDARD -> warning("watchdog", "Safety watchdog", "Fail-Open is active; interception remains paused.")
            else -> blocker("watchdog", "Safety watchdog", "Wait for Fail-Open recovery to end before Strict or Hard activation.")
        }
    }

    private fun notifications(): PreflightCheck {
        val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return if (granted) pass("notifications", "Status notifications", "Notification permission is available.")
        else warning("notifications", "Status notifications", "Permission will be requested after activation.")
    }

    @Suppress("DEPRECATION")
    private fun isInstalled(packageName: String): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= 33) context.packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        else context.packageManager.getApplicationInfo(packageName, 0)
    }.isSuccess

    private fun pass(id: String, label: String, detail: String) = PreflightCheck(id, label, CheckSeverity.PASS, detail)
    private fun warning(id: String, label: String, detail: String) = PreflightCheck(id, label, CheckSeverity.WARNING, detail)
    private fun blocker(id: String, label: String, detail: String) = PreflightCheck(id, label, CheckSeverity.BLOCKER, detail)
}
