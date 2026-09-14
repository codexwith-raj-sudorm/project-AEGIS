package com.jarvis.aegis.challenge

data class EconomyState(val streak: Int, val tokens: Int)

object EconomyPolicy {
    fun correct(current: EconomyState, rewardAt: Int = 20): EconomyState {
        require(rewardAt > 0)
        val next = current.streak.coerceAtLeast(0) + 1
        return if (next >= rewardAt) EconomyState(0, current.tokens.coerceAtLeast(0) + 1)
        else EconomyState(next, current.tokens.coerceAtLeast(0))
    }

    fun spend(current: EconomyState): EconomyState? =
        if (current.tokens > 0) current.copy(tokens = current.tokens - 1) else null
}
