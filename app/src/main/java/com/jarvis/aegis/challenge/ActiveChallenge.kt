package com.jarvis.aegis.challenge

import com.jarvis.aegis.session.ClockSnapshot
import com.jarvis.aegis.session.DeadlineState
import com.jarvis.aegis.session.TimeAnchor
import com.jarvis.aegis.session.TimeIntegrity
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class ActiveChallenge(
    val sessionId: UUID,
    val targetPackage: String,
    val challenge: Challenge,
    val issuedAt: Instant,
    val deadline: Instant,
    val timeAnchor: TimeAnchor? = null,
) {
    fun isValidFor(session: UUID, target: String, now: Instant): Boolean =
        sessionId == session && targetPackage == target && now.isBefore(deadline)

    fun isValidFor(session: UUID, target: String, now: ClockSnapshot): Boolean =
        sessionId == session && targetPackage == target && deadlineState(now) == DeadlineState.ACTIVE

    fun remainingSeconds(now: Instant): Int =
        (deadline.epochSecond - now.epochSecond).coerceAtLeast(0).toInt()

    fun remainingSeconds(now: ClockSnapshot): Int = timeAnchor?.let {
        TimeIntegrity.remaining(it, Duration.ofSeconds(challenge.timeLimitSeconds.toLong()), now)?.seconds?.toInt()
    } ?: remainingSeconds(now.wallTime)

    fun deadlineState(now: ClockSnapshot): DeadlineState = timeAnchor?.let {
        TimeIntegrity.evaluate(it, Duration.ofSeconds(challenge.timeLimitSeconds.toLong()), now)
    } ?: if (now.wallTime.isBefore(deadline)) DeadlineState.ACTIVE else DeadlineState.EXPIRED
}
