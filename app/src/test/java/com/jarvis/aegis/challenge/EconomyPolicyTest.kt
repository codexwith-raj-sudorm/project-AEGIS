package com.jarvis.aegis.challenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EconomyPolicyTest {
    @Test fun twentiethAnswerResetsStreakAndAwardsExactlyOneToken() {
        assertEquals(EconomyState(0, 4), EconomyPolicy.correct(EconomyState(19, 3)))
    }

    @Test fun ordinaryCorrectAnswerOnlyIncrementsStreak() {
        assertEquals(EconomyState(8, 2), EconomyPolicy.correct(EconomyState(7, 2)))
    }

    @Test fun spendingCannotCreateNegativeBalance() {
        assertNull(EconomyPolicy.spend(EconomyState(5, 0)))
        assertEquals(EconomyState(5, 1), EconomyPolicy.spend(EconomyState(5, 2)))
    }
}
