package com.jarvis.aegis.recovery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryCodeManagerTest {
    @Test fun acceptsGeneratedCodeAndRejectsWrongCode() {
        val manager = RecoveryCodeManager(iterations = 100_000)
        val enrollment = manager.enroll()
        assertTrue(manager.verify(enrollment.displayCode.toCharArray(), enrollment.verifier))
        assertFalse(manager.verify("WRONG-CODE".toCharArray(), enrollment.verifier))
    }
}
