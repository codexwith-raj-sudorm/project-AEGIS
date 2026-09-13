package com.jarvis.aegis.session

import java.time.Duration
import java.time.Instant
import kotlin.math.min

data class LaunchAttemptState(
    val attempts: Int = 0,
    val windowStartedAt: Instant = Instant.EPOCH,
    val cooldownUntil: Instant = Instant.EPOCH,
)

class LaunchCooldownPolicy {
    fun register(previous: LaunchAttemptState, now: Instant): LaunchAttemptState {
        val attempts = if (Duration.between(previous.windowStartedAt, now) > WINDOW) 1 else previous.attempts + 1
        val started = if (attempts == 1) now else previous.windowStartedAt
        val seconds = when (attempts) {
            1, 2 -> 0L
            3 -> 5L
            4 -> 15L
            5 -> 30L
            else -> min(300L, 30L * (attempts - 4))
        }
        return LaunchAttemptState(attempts, started, now.plusSeconds(seconds))
    }

    fun remaining(state: LaunchAttemptState, now: Instant): Duration =
        Duration.between(now, state.cooldownUntil).coerceAtLeast(Duration.ZERO)

    companion object { private val WINDOW = Duration.ofMinutes(10) }
}
