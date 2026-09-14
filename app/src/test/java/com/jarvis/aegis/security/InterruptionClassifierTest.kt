package com.jarvis.aegis.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterruptionClassifierTest {
    private val classifier = InterruptionClassifier(
        aegisPackage = "com.jarvis.aegis",
        essentialPackages = setOf("phone.app", "keyboard.app"),
        inputMethodPackage = "keyboard.app",
    )

    @Test fun permitsSystemAuthenticationAndKeyboardSurfaces() {
        assertFalse(classifier.shouldInvalidate("com.android.systemui"))
        assertFalse(classifier.shouldInvalidate("com.android.permissioncontroller"))
        assertFalse(classifier.shouldInvalidate("com.android.biometric"))
        assertFalse(classifier.shouldInvalidate("keyboard.app"))
        assertFalse(classifier.shouldInvalidate("phone.app"))
    }

    @Test fun invalidatesUnrelatedUserApp() {
        assertTrue(classifier.shouldInvalidate("browser.app"))
    }

    @Test fun identifiesAegisItself() {
        assertEquals(InterruptionKind.AEGIS, classifier.classify("com.jarvis.aegis"))
    }
}
