package com.jarvis.aegis.session

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingRulesTest {
    @Test fun modesHaveProgressivelyStricterExitPolicies() {
        assertEquals(ExitPolicy.IMMEDIATE, SessionMode.STANDARD.rules.exitPolicy)
        assertEquals(ExitPolicy.DELAYED_OR_KEY, SessionMode.STRICT.rules.exitPolicy)
        assertEquals(ExitPolicy.KEY_ONLY, SessionMode.EXTREME.rules.exitPolicy)
    }

    @Test fun hardModeDisablesAmnestyAndRequiresAuthentication() {
        assertFalse(SessionMode.EXTREME.rules.allowAmnesty)
        assertTrue(SessionMode.EXTREME.rules.requireDeviceAuthentication)
    }

    @Test fun cooldownStrengthTracksMode() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val policy = LaunchCooldownPolicy()
        fun third(mode: SessionMode): Long {
            var state = LaunchAttemptState()
            repeat(3) { state = policy.register(state, now.plusSeconds(it.toLong()), mode) }
            return policy.remaining(state, now.plusSeconds(2)).seconds
        }
        assertEquals(0, third(SessionMode.STANDARD))
        assertEquals(5, third(SessionMode.STRICT))
        assertEquals(10, third(SessionMode.EXTREME))
    }
}
