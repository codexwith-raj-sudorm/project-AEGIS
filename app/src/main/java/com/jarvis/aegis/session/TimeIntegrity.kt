package com.jarvis.aegis.session

import java.time.Duration
import java.time.Instant
import kotlin.math.abs

data class ClockSnapshot(val wallTime: Instant, val elapsedRealtimeMs: Long, val bootCount: Int)
data class TimeAnchor(val wallTime: Instant, val elapsedRealtimeMs: Long, val bootCount: Int)
enum class DeadlineState { ACTIVE, EXPIRED, INVALID }

object TimeIntegrity {
    private val maxWallDrift = Duration.ofMinutes(5)
    private val rebootRollbackTolerance = Duration.ofMinutes(2)

    fun evaluate(anchor: TimeAnchor, duration: Duration, now: ClockSnapshot): DeadlineState {
        if (duration.isZero || duration.isNegative) return DeadlineState.INVALID
        val wallDeadline = anchor.wallTime.plus(duration)
        if (now.bootCount == anchor.bootCount) {
            val elapsedDelta = now.elapsedRealtimeMs - anchor.elapsedRealtimeMs
            if (elapsedDelta < 0) return DeadlineState.INVALID
            val expectedWall = anchor.wallTime.plusMillis(elapsedDelta)
            if (abs(Duration.between(expectedWall, now.wallTime).toMillis()) > maxWallDrift.toMillis()) {
                return DeadlineState.INVALID
            }
            if (elapsedDelta >= duration.toMillis() || !now.wallTime.isBefore(wallDeadline)) return DeadlineState.EXPIRED
            return DeadlineState.ACTIVE
        }
        if (now.wallTime.isBefore(anchor.wallTime.minus(rebootRollbackTolerance))) return DeadlineState.INVALID
        return if (now.wallTime.isBefore(wallDeadline)) DeadlineState.ACTIVE else DeadlineState.EXPIRED
    }

    fun remaining(anchor: TimeAnchor, duration: Duration, now: ClockSnapshot): Duration? {
        if (evaluate(anchor, duration, now) != DeadlineState.ACTIVE) return null
        return if (now.bootCount == anchor.bootCount) {
            duration.minusMillis(now.elapsedRealtimeMs - anchor.elapsedRealtimeMs).coerceAtLeast(Duration.ZERO)
        } else Duration.between(now.wallTime, anchor.wallTime.plus(duration)).coerceAtLeast(Duration.ZERO)
    }
}
