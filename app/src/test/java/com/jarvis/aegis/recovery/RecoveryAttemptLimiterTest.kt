package com.jarvis.aegis.recovery

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryAttemptLimiterTest {
    @Test fun appliesBackoffAfterThirdFailure() {
        val limiter = RecoveryAttemptLimiter()
        val now = Instant.parse("2026-01-01T00:00:00Z")
        var state = AttemptState()
        repeat(3) { state = limiter.onFailure(state, now) }
        assertFalse(limiter.decision(state, now.plusSeconds(4)).allowed)
        assertTrue(limiter.decision(state, now.plusSeconds(5)).allowed)
    }

    @Test fun successfulRecoveryClearsFailures() {
        assertTrue(limiterDecisionAfterSuccess())
    }

    private fun limiterDecisionAfterSuccess(): Boolean {
        val limiter = RecoveryAttemptLimiter()
        return limiter.decision(limiter.onSuccess(), Instant.EPOCH).allowed
    }
}
