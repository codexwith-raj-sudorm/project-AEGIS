package com.jarvis.aegis.session

import java.time.Clock
import java.time.Instant

/** In-memory milestone implementation. Persistent encrypted storage follows in Phase 1. */
class FocusSessionManager(private val clock: Clock = Clock.systemUTC()) {
    var activeSession: FocusSession? = null
        private set

    fun start(policy: SessionPolicy): FocusSession = FocusSession(
        policy = policy,
        activatedAt = Instant.now(clock),
    ).also { activeSession = it }

    fun current(): FocusSession? = activeSession?.takeIf { it.isActive(Instant.now(clock)) }
        .also { if (it == null) activeSession = null }

    fun interrupt() {
        activeSession = activeSession?.copy(interrupted = true)
    }

    fun clearExpired() = current()
}
