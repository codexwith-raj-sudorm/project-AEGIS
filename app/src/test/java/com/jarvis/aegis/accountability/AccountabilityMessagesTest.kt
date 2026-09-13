package com.jarvis.aegis.accountability

import com.jarvis.aegis.profile.AgeBand
import com.jarvis.aegis.profile.MotivationProfile
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AccountabilityMessagesTest {
    @Test fun hardTruthIsNotUsedForMinors() {
        val messages = AccountabilityMessages()
        val adult = messages.message(MotivationProfile.HARD_TRUTH, AgeBand.ADULT, AccountabilityEvent.OVERRIDE_REQUEST)
        val minor = messages.message(MotivationProfile.HARD_TRUTH, AgeBand.TEEN, AccountabilityEvent.OVERRIDE_REQUEST)
        assertNotEquals(adult, minor)
    }
}
