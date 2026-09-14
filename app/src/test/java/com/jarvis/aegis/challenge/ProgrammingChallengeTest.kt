package com.jarvis.aegis.challenge

import com.jarvis.aegis.session.ChallengeDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgrammingChallengeTest {
    private val generator = ChallengeGenerator()
    private val validator = AnswerValidator()

    @Test fun computerScienceSelectionNeverFallsBackToMath() {
        repeat(40) {
            val challenge = generator.generate(setOf("Computer Science"), ChallengeDifficulty.INTERMEDIATE)
            assertTrue(challenge.category in setOf(ChallengeCategory.JAVA, ChallengeCategory.KOTLIN, ChallengeCategory.PYTHON))
        }
    }

    @Test fun createsEveryProgrammingLanguage() {
        assertEquals(ProgrammingLanguage.JAVA, generator.javaOutput(ChallengeDifficulty.FOUNDATION).language)
        assertEquals(ProgrammingLanguage.KOTLIN, generator.kotlinOutput(ChallengeDifficulty.INTERMEDIATE).language)
        assertEquals(ProgrammingLanguage.PYTHON, generator.pythonOutput(ChallengeDifficulty.ADVANCED).language)
    }

    @Test fun outputValidationNormalizesLineEndingsAndSpacing() {
        val challenge = CodeOutputChallenge(
            category = ChallengeCategory.JAVA,
            language = ProgrammingLanguage.JAVA,
            prompt = "output?",
            timeLimitSeconds = 30,
            expectedOutput = "1 2 3\nDONE",
        )
        assertEquals(ValidationResult.Correct, validator.validate(challenge, " 1   2  3\r\nDONE "))
        assertEquals(ValidationResult.Incorrect(null), validator.validate(challenge, "1 2 4\nDONE"))
    }
}
