package com.jarvis.aegis.session

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchCooldownPolicyTest {
    private val policy = LaunchCooldownPolicy()
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    @Test fun thirdRapidLaunchStartsFiveSecondCooldown() {
        var state = LaunchAttemptState()
        repeat(3) { state = policy.register(state, now.plusSeconds(it.toLong())) }
        assertEquals(5, policy.remaining(state, now.plusSeconds(2)).seconds)
    }

    @Test fun attemptsResetAfterWindow() {
        val old = LaunchAttemptState(9, now, now.plusSeconds(300))
        val reset = policy.register(old, now.plusSeconds(601))
        assertEquals(1, reset.attempts)
        assertEquals(0, policy.remaining(reset, now.plusSeconds(601)).seconds)
    }

    @Test fun cooldownIsCappedAtFiveMinutes() {
        var state = LaunchAttemptState()
        repeat(20) { state = policy.register(state, now) }
        assertEquals(300, policy.remaining(state, now).seconds)
    }
}
