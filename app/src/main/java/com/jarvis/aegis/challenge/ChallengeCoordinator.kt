package com.jarvis.aegis.challenge

import com.jarvis.aegis.data.AegisStore
import com.jarvis.aegis.session.FocusSession
import java.time.Instant

class ChallengeCoordinator(
    private val store: AegisStore,
    private val generator: ChallengeGenerator = ChallengeGenerator(),
) {
    fun currentOrCreate(session: FocusSession, target: String, now: Instant = Instant.now()): ActiveChallenge {
        store.activeChallenge()?.takeIf { it.isValidFor(session.id, target, now) }?.let { return it }
        store.clearActiveChallenge()
        val challenge = generator.generate(session.policy.challengeSubjects, session.policy.difficulty)
        return ActiveChallenge(
            sessionId = session.id,
            targetPackage = target,
            challenge = challenge,
            issuedAt = now,
            deadline = now.plusSeconds(challenge.timeLimitSeconds.toLong()),
        ).also(store::saveActiveChallenge)
    }

    fun replace(session: FocusSession, target: String, now: Instant = Instant.now()): ActiveChallenge {
        store.clearActiveChallenge()
        return currentOrCreate(session, target, now)
    }

    fun consume(active: ActiveChallenge): Boolean = store.consumeActiveChallenge(active.challenge.id)
}
