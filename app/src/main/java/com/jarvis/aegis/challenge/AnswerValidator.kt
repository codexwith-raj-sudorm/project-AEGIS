package com.jarvis.aegis.challenge

import kotlin.math.abs
import kotlin.math.max

sealed interface ValidationResult {
    data object Correct : ValidationResult
    data class Incorrect(val expectedUnit: String?) : ValidationResult
    data object InvalidFormat : ValidationResult
}

class AnswerValidator {
    fun validate(challenge: NumericChallenge, rawAnswer: String): ValidationResult {
        val normalized = rawAnswer.trim().lowercase()
        val number = NUMBER.find(normalized)?.value?.toDoubleOrNull()
            ?: return ValidationResult.InvalidFormat
        val difference = abs(number - challenge.expected)
        val tolerance = max(
            challenge.absoluteTolerance,
            abs(challenge.expected) * challenge.relativeTolerance,
        )
        return if (difference <= tolerance) ValidationResult.Correct
        else ValidationResult.Incorrect(challenge.unit)
    }

    private companion object {
        val NUMBER = Regex("[-+]?(?:\\d+\\.?\\d*|\\.\\d+)")
    }
}
