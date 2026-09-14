package com.jarvis.aegis.security

enum class InterruptionKind { AEGIS, ESSENTIAL_SYSTEM, INPUT_METHOD, AUTHENTICATION, USER_APP }

class InterruptionClassifier(
    private val aegisPackage: String,
    private val essentialPackages: Set<String>,
    private val inputMethodPackage: String? = null,
) {
    fun classify(packageName: String?): InterruptionKind = when {
        packageName == null -> InterruptionKind.ESSENTIAL_SYSTEM
        packageName == aegisPackage -> InterruptionKind.AEGIS
        packageName == inputMethodPackage -> InterruptionKind.INPUT_METHOD
        packageName in AUTH_PACKAGES || packageName.contains("biometric", ignoreCase = true) -> InterruptionKind.AUTHENTICATION
        packageName in essentialPackages || packageName in SYSTEM_PACKAGES -> InterruptionKind.ESSENTIAL_SYSTEM
        else -> InterruptionKind.USER_APP
    }

    fun shouldInvalidate(packageName: String?): Boolean = classify(packageName) == InterruptionKind.USER_APP

    companion object {
        private val AUTH_PACKAGES = setOf(
            "com.android.settings.intelligence",
            "com.android.keyguard",
            "com.google.android.gms",
        )
        private val SYSTEM_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
        )
    }
}
