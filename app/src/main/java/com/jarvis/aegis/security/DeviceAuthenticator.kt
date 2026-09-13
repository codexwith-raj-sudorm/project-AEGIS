package com.jarvis.aegis.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class DeviceAuthenticator(private val activity: FragmentActivity) {
    fun authenticate(onResult: (Boolean, String?) -> Unit) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val availability = BiometricManager.from(activity).canAuthenticate(authenticators)
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            onResult(false, "DEVICE AUTHENTICATION IS NOT CONFIGURED.")
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true, null)
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(false, errString.toString())
                override fun onAuthenticationFailed() = Unit
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authorize AEGIS session")
                .setSubtitle("Confirm strict session activation")
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }
}
