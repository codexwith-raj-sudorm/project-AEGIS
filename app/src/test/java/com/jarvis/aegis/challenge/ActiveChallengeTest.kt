package com.jarvis.aegis.challenge

import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveChallengeTest {
    private val sessionId = UUID.randomUUID()
    private val challenge = NumericChallenge(prompt = "2 + 2", timeLimitSeconds = 20, expected = 4.0)
    private val issued = Instant.parse("2026-01-01T00:00:00Z")
    private val active = ActiveChallenge(sessionId, "target.app", challenge, issued, issued.plusSeconds(20))

    @Test fun restoresRemainingTimeFromPersistedDeadline() {
        assertEquals(13, active.remainingSeconds(issued.plusSeconds(7)))
        assertEquals(0, active.remainingSeconds(issued.plusSeconds(30)))
    }

    @Test fun validatesSessionTargetAndDeadline() {
        assertTrue(active.isValidFor(sessionId, "target.app", issued.plusSeconds(19)))
        assertFalse(active.isValidFor(sessionId, "other.app", issued.plusSeconds(19)))
        assertFalse(active.isValidFor(sessionId, "target.app", issued.plusSeconds(20)))
    }
}
