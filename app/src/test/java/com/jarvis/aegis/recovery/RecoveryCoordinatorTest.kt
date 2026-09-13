package com.jarvis.aegis.recovery

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryCoordinatorTest {
    @Test fun delayedExitBecomesReadyAtDeadline() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val exit = RecoveryCoordinator().request(now, Duration.ofMinutes(5))
        assertFalse(exit.isReady(now.plusSeconds(299)))
        assertTrue(exit.isReady(now.plusSeconds(300)))
    }
}
