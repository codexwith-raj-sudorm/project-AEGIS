package com.jarvis.aegis.challenge

import com.jarvis.aegis.data.AegisStore
import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class GateTransactionStatus { APPLIED, REPLAY_REJECTED, STALE_CHALLENGE, NO_SESSION, NO_TOKEN }
data class GateTransactionResult(val status: GateTransactionStatus, val streak: Int = 0, val tokens: Int = 0)

/** Serializes all economy-changing gate operations and leaves an idempotent recovery journal. */
class GateTransactionCoordinator(private val store: AegisStore) {
    fun acceptCorrect(challengeId: UUID, target: String, grant: Duration): GateTransactionResult = synchronized(LOCK) {
        store.recoverPendingGateTransaction()
        if (store.wasChallengeCompleted(challengeId)) return@synchronized GateTransactionResult(GateTransactionStatus.REPLAY_REJECTED)
        val active = store.activeChallenge()
        if (active?.challenge?.id != challengeId) return@synchronized GateTransactionResult(GateTransactionStatus.STALE_CHALLENGE)
        val session = store.activeSession() ?: return@synchronized GateTransactionResult(GateTransactionStatus.NO_SESSION)
        val economy = EconomyPolicy.correct(EconomyState(session.streak, store.tokens()))
        val finalStreak = economy.streak
        val finalTokens = economy.tokens
        val transaction = GateTransaction(
            id = UUID.randomUUID(), challengeId = challengeId, sessionId = session.id,
            target = target, finalStreak = finalStreak, finalTokens = finalTokens,
            grantUntil = store.clockSnapshot().wallTime.plus(grant), spendToken = false,
        )
        store.saveGateTransaction(transaction)
        store.applyGateTransaction(transaction)
        GateTransactionResult(GateTransactionStatus.APPLIED, finalStreak, finalTokens)
    }

    fun spendToken(challengeId: UUID, target: String, grant: Duration): GateTransactionResult = synchronized(LOCK) {
        store.recoverPendingGateTransaction()
        val active = store.activeChallenge()
        if (active?.challenge?.id != challengeId) return@synchronized GateTransactionResult(GateTransactionStatus.STALE_CHALLENGE)
        val session = store.activeSession() ?: return@synchronized GateTransactionResult(GateTransactionStatus.NO_SESSION)
        val economy = EconomyPolicy.spend(EconomyState(session.streak, store.tokens()))
            ?: return@synchronized GateTransactionResult(GateTransactionStatus.NO_TOKEN)
        val transaction = GateTransaction(
            id = UUID.randomUUID(), challengeId = challengeId, sessionId = session.id,
            target = target, finalStreak = economy.streak, finalTokens = economy.tokens,
            grantUntil = store.clockSnapshot().wallTime.plus(grant), spendToken = true,
        )
        store.saveGateTransaction(transaction)
        store.applyGateTransaction(transaction)
        GateTransactionResult(GateTransactionStatus.APPLIED, economy.streak, economy.tokens)
    }

    companion object { private val LOCK = Any() }
}

data class GateTransaction(
    val id: UUID,
    val challengeId: UUID,
    val sessionId: UUID,
    val target: String,
    val finalStreak: Int,
    val finalTokens: Int,
    val grantUntil: Instant,
    val spendToken: Boolean,
)
