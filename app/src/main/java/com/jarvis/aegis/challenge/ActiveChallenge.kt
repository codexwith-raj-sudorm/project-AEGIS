package com.jarvis.aegis.challenge

import java.time.Instant
import java.util.UUID

data class ActiveChallenge(
    val sessionId: UUID,
    val targetPackage: String,
    val challenge: NumericChallenge,
    val issuedAt: Instant,
    val deadline: Instant,
) {
    fun isValidFor(session: UUID, target: String, now: Instant): Boolean =
        sessionId == session && targetPackage == target && now.isBefore(deadline)

    fun remainingSeconds(now: Instant): Int =
        (deadline.epochSecond - now.epochSecond).coerceAtLeast(0).toInt()
}
