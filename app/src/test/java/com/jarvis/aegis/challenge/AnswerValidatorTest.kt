package com.jarvis.aegis.challenge

import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerValidatorTest {
    private val validator = AnswerValidator()
    private val challenge = NumericChallenge(prompt = "test", timeLimitSeconds = 20, expected = 47.68)

    @Test fun acceptsExactAnswer() = assertEquals(ValidationResult.Correct, validator.validate(challenge, "47.68"))
    @Test fun acceptsAbsoluteTolerance() = assertEquals(ValidationResult.Correct, validator.validate(challenge, "47.72 m/s"))
    @Test fun rejectsBadInput() = assertEquals(ValidationResult.InvalidFormat, validator.validate(challenge, "unknown"))

    @Test fun rejectsWrongSuppliedUnit() {
        val measured = challenge.copy(unit = "m/s")
        assertEquals(ValidationResult.IncorrectUnit("m/s"), validator.validate(measured, "47.68 kg"))
    }

    @Test fun acceptsEquivalentUnitName() {
        val measured = challenge.copy(unit = "m")
        assertEquals(ValidationResult.Correct, validator.validate(measured, "47.68 metres"))
    }
}
