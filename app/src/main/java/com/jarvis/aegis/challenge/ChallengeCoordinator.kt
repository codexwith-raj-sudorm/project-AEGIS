package com.jarvis.aegis.challenge

import com.jarvis.aegis.data.AegisStore
import com.jarvis.aegis.session.ClockSnapshot
import com.jarvis.aegis.session.FocusSession
import com.jarvis.aegis.session.TimeAnchor

class ChallengeCoordinator(
    private val store: AegisStore,
    private val generator: ChallengeGenerator = ChallengeGenerator(),
) {
    fun currentOrCreate(
        session: FocusSession,
        target: String,
        now: ClockSnapshot = store.clockSnapshot(),
    ): ActiveChallenge {
        store.activeChallenge()?.takeIf { it.isValidFor(session.id, target, now) }?.let { return it }
        store.clearActiveChallenge()
        val challenge = generator.generate(session.policy.challengeSubjects, session.policy.difficulty)
        return ActiveChallenge(
            sessionId = session.id,
            targetPackage = target,
            challenge = challenge,
            issuedAt = now.wallTime,
            deadline = now.wallTime.plusSeconds(challenge.timeLimitSeconds.toLong()),
            timeAnchor = TimeAnchor(now.wallTime, now.elapsedRealtimeMs, now.bootCount),
        ).also(store::saveActiveChallenge)
    }

    fun replace(session: FocusSession, target: String): ActiveChallenge {
        store.clearActiveChallenge()
        return currentOrCreate(session, target)
    }

    fun consume(active: ActiveChallenge): Boolean = store.consumeActiveChallenge(active.challenge.id)
}
