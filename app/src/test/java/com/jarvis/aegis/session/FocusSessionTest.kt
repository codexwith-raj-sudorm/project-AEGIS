package com.jarvis.aegis.session

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSessionTest {
    private val policy = SessionPolicy(
        mode = SessionMode.EXTREME,
        duration = Duration.ofHours(1),
        targetPackages = setOf("example.target"),
        essentialPackages = setOf("example.phone"),
    )

    @Test fun activeBeforeExpiry() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val session = FocusSession(policy = policy, activatedAt = start)
        assertTrue(session.isActive(start.plusSeconds(3599)))
        assertFalse(session.isActive(start.plusSeconds(3600)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEssentialTargetOverlap() {
        SessionPolicy(
            mode = SessionMode.EXTREME,
            duration = Duration.ofHours(1),
            targetPackages = setOf("example.phone"),
            essentialPackages = setOf("example.phone"),
        )
    }
}
