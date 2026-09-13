package com.jarvis.aegis.recovery

import java.time.Duration
import java.time.Instant
import kotlin.math.min

data class AttemptState(val failures: Int = 0, val blockedUntil: Instant? = null)

data class AttemptDecision(val allowed: Boolean, val remaining: Duration = Duration.ZERO)

class RecoveryAttemptLimiter {
    fun decision(state: AttemptState, now: Instant): AttemptDecision {
        val until = state.blockedUntil ?: return AttemptDecision(true)
        return if (now.isBefore(until)) AttemptDecision(false, Duration.between(now, until))
        else AttemptDecision(true)
    }

    fun onFailure(state: AttemptState, now: Instant): AttemptState {
        val failures = state.failures + 1
        // First two attempts are immediate; then 5s, 10s, 20s... capped at 15 minutes.
        val delaySeconds = if (failures < 3) 0L else min(900L, 5L shl min(failures - 3, 8))
        return AttemptState(failures, now.plusSeconds(delaySeconds))
    }

    fun onSuccess(): AttemptState = AttemptState()
}
