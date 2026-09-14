package com.jarvis.aegis.session

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeIntegrityTest {
    private val wall = Instant.parse("2026-01-01T00:00:00Z")
    private val anchor = TimeAnchor(wall, 10_000, 7)
    private val duration = Duration.ofMinutes(10)

    @Test fun sameBootUsesMonotonicElapsedTime() {
        val now = ClockSnapshot(wall.plusSeconds(120), 130_000, 7)
        assertEquals(DeadlineState.ACTIVE, TimeIntegrity.evaluate(anchor, duration, now))
        assertEquals(480, TimeIntegrity.remaining(anchor, duration, now)?.seconds)
    }

    @Test fun detectsWallClockRollbackOnSameBoot() {
        val now = ClockSnapshot(wall.minusSeconds(600), 70_000, 7)
        assertEquals(DeadlineState.INVALID, TimeIntegrity.evaluate(anchor, duration, now))
    }

    @Test fun detectsElapsedRealtimeRollback() {
        val now = ClockSnapshot(wall, 9_999, 7)
        assertEquals(DeadlineState.INVALID, TimeIntegrity.evaluate(anchor, duration, now))
    }

    @Test fun expiresFromMonotonicTimeDespiteSmallWallDrift() {
        val now = ClockSnapshot(wall.plusSeconds(590), 610_000, 7)
        assertEquals(DeadlineState.EXPIRED, TimeIntegrity.evaluate(anchor, duration, now))
    }

    @Test fun differentBootFallsBackToConservativeWallDeadline() {
        assertEquals(DeadlineState.ACTIVE, TimeIntegrity.evaluate(anchor, duration, ClockSnapshot(wall.plusSeconds(300), 5_000, 8)))
        assertEquals(DeadlineState.EXPIRED, TimeIntegrity.evaluate(anchor, duration, ClockSnapshot(wall.plusSeconds(600), 5_000, 8)))
    }

    @Test fun rebootWithClockBeforeActivationIsInvalid() {
        assertEquals(DeadlineState.INVALID, TimeIntegrity.evaluate(anchor, duration, ClockSnapshot(wall.minusSeconds(180), 5_000, 8)))
    }
}
