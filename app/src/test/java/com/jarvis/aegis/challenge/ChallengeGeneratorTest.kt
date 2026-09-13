package com.jarvis.aegis.challenge

import com.jarvis.aegis.session.ChallengeDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeGeneratorTest {
    private val generator = ChallengeGenerator()

    @Test fun generatesAllNumericalCategories() {
        assertEquals(ChallengeCategory.ARITHMETIC, generator.arithmetic().category)
        assertEquals(ChallengeCategory.ALGEBRA, generator.algebra(ChallengeDifficulty.INTERMEDIATE).category)
        assertEquals(ChallengeCategory.LOGIC, generator.logic(ChallengeDifficulty.INTERMEDIATE).category)
        assertEquals(ChallengeCategory.KINEMATICS, generator.kinematics().category)
        assertEquals(ChallengeCategory.CHEMISTRY, generator.chemistry(ChallengeDifficulty.INTERMEDIATE).category)
        assertEquals(ChallengeCategory.UNIT_CONVERSION, generator.unitConversion(ChallengeDifficulty.INTERMEDIATE).category)
        assertEquals(ChallengeCategory.MATRIX, generator.matrix(ChallengeDifficulty.INTERMEDIATE).category)
    }

    @Test fun physicsSelectionStaysInEligibleCategories() {
        repeat(30) {
            assertTrue(generator.generate(setOf("Physics"), ChallengeDifficulty.ADVANCED).category in setOf(ChallengeCategory.KINEMATICS, ChallengeCategory.UNIT_CONVERSION))
        }
    }

    @Test fun generatedIdsAreUnique() {
        assertNotEquals(generator.arithmetic().id, generator.arithmetic().id)
    }
}
