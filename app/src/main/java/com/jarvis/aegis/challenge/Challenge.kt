package com.jarvis.aegis.challenge

import java.util.UUID

enum class ChallengeCategory {
    ARITHMETIC, ALGEBRA, LOGIC, KINEMATICS, CHEMISTRY, UNIT_CONVERSION, MATRIX,
    JAVA, KOTLIN, PYTHON,
}

enum class ProgrammingLanguage { JAVA, KOTLIN, PYTHON }

sealed interface Challenge {
    val id: UUID
    val category: ChallengeCategory
    val prompt: String
    val timeLimitSeconds: Int
    val signature: String
}

data class NumericChallenge(
    override val id: UUID = UUID.randomUUID(),
    override val category: ChallengeCategory = ChallengeCategory.ARITHMETIC,
    override val prompt: String,
    override val timeLimitSeconds: Int,
    override val signature: String = prompt,
    val expected: Double,
    val unit: String? = null,
    val absoluteTolerance: Double = 0.05,
    val relativeTolerance: Double = 0.001,
) : Challenge

data class CodeOutputChallenge(
    override val id: UUID = UUID.randomUUID(),
    override val category: ChallengeCategory,
    override val prompt: String,
    override val timeLimitSeconds: Int,
    override val signature: String = prompt,
    val language: ProgrammingLanguage,
    val expectedOutput: String,
) : Challenge
