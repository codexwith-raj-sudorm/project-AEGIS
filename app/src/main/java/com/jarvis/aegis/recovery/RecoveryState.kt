package com.jarvis.aegis.recovery

import java.time.Duration
import java.time.Instant

data class DelayedExit(val requestedAt: Instant, val availableAt: Instant) {
    fun remaining(now: Instant): Duration = Duration.between(now, availableAt).coerceAtLeast(Duration.ZERO)
    fun isReady(now: Instant): Boolean = !now.isBefore(availableAt)
}

class RecoveryCoordinator {
    fun request(now: Instant, delay: Duration): DelayedExit {
        require(delay in Duration.ofMinutes(1)..Duration.ofMinutes(10))
        return DelayedExit(now, now.plus(delay))
    }
}
