package com.jarvis.aegis.challenge

import kotlin.math.abs
import kotlin.math.max

sealed interface ValidationResult {
    data object Correct : ValidationResult
    data class Incorrect(val expectedUnit: String?) : ValidationResult
    data class IncorrectUnit(val expectedUnit: String) : ValidationResult
    data object InvalidFormat : ValidationResult
}

class AnswerValidator {
    fun validate(challenge: NumericChallenge, rawAnswer: String): ValidationResult {
        val normalized = rawAnswer.trim().lowercase()
        val match = NUMBER.find(normalized) ?: return ValidationResult.InvalidFormat
        val number = match.value.toDoubleOrNull() ?: return ValidationResult.InvalidFormat
        val suppliedUnit = normalized.removeRange(match.range).trim().replace(" ", "")
        if (challenge.unit != null && suppliedUnit.isNotEmpty() && normalizeUnit(suppliedUnit) != normalizeUnit(challenge.unit)) {
            return ValidationResult.IncorrectUnit(challenge.unit)
        }
        val difference = abs(number - challenge.expected)
        val tolerance = max(challenge.absoluteTolerance, abs(challenge.expected) * challenge.relativeTolerance)
        return if (difference <= tolerance) ValidationResult.Correct else ValidationResult.Incorrect(challenge.unit)
    }

    private fun normalizeUnit(unit: String): String = when (unit.lowercase().replace(" ", "")) {
        "meter", "meters", "metre", "metres" -> "m"
        "meter/second", "metre/second", "meters/second", "metres/second", "ms^-1", "ms⁻¹" -> "m/s"
        "gram", "grams" -> "g"
        else -> unit.lowercase().replace(" ", "")
    }

    private companion object {
        val NUMBER = Regex("[-+]?(?:\\d+\\.?\\d*|\\.\\d+)")
    }
}
