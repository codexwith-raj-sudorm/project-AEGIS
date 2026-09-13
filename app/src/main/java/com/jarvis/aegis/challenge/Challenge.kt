package com.jarvis.aegis.challenge

import java.util.UUID

sealed interface Challenge {
    val id: UUID
    val prompt: String
    val timeLimitSeconds: Int
}

data class NumericChallenge(
    override val id: UUID = UUID.randomUUID(),
    override val prompt: String,
    override val timeLimitSeconds: Int,
    val expected: Double,
    val unit: String? = null,
    val absoluteTolerance: Double = 0.05,
    val relativeTolerance: Double = 0.001,
) : Challenge
