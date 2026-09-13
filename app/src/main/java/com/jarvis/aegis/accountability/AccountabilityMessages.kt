package com.jarvis.aegis.accountability

import com.jarvis.aegis.profile.AgeBand
import com.jarvis.aegis.profile.MotivationProfile

enum class AccountabilityEvent { CONTEXT_SHIFT, TIMEOUT, OVERRIDE_REQUEST }

class AccountabilityMessages {
    fun message(profile: MotivationProfile, ageBand: AgeBand, event: AccountabilityEvent): String {
        val safeProfile = if (ageBand == AgeBand.ADULT) profile else profile.takeUnless { it == MotivationProfile.HARD_TRUTH } ?: MotivationProfile.REFLECTIVE
        return messages.getValue(safeProfile).getValue(event)
    }

    private val messages = mapOf(
        MotivationProfile.NEUTRAL to mapOf(
            AccountabilityEvent.CONTEXT_SHIFT to "CONTEXT SHIFT DETECTED. CHALLENGE INVALIDATED.",
            AccountabilityEvent.TIMEOUT to "TIME LIMIT EXPIRED. STREAK RESET.",
            AccountabilityEvent.OVERRIDE_REQUEST to "SESSION OVERRIDE REQUESTED.",
        ),
        MotivationProfile.REFLECTIVE to mapOf(
            AccountabilityEvent.CONTEXT_SHIFT to "YOU ACTIVATED AEGIS BECAUSE THIS MOMENT WAS PREDICTABLE.",
            AccountabilityEvent.TIMEOUT to "PAUSE. TRY THE NEXT CHALLENGE WITH FULL ATTENTION.",
            AccountabilityEvent.OVERRIDE_REQUEST to "DO YOU REALLY WANT TO ABANDON THIS SESSION?",
        ),
        MotivationProfile.CHALLENGER to mapOf(
            AccountabilityEvent.CONTEXT_SHIFT to "ONE DISTRACTION DOES NOT DEFINE THE SESSION. RETURN AND HOLD THE LINE.",
            AccountabilityEvent.TIMEOUT to "IS THIS YOUR LIMIT, OR WILL YOU COMPLETE THE NEXT ONE?",
            AccountabilityEvent.OVERRIDE_REQUEST to "THE DISTRACTION CAN WAIT. PROVE THAT YOUR COMMITMENT CANNOT.",
        ),
        MotivationProfile.HARD_TRUTH to mapOf(
            AccountabilityEvent.CONTEXT_SHIFT to "THIS IMPULSE HAS DEFEATED YOUR PLANS BEFORE. IT IS HAPPENING AGAIN.",
            AccountabilityEvent.TIMEOUT to "THE RESULT WILL NOT CHANGE UNTIL THE ACTION CHANGES.",
            AccountabilityEvent.OVERRIDE_REQUEST to "ENDING THIS SESSION GIVES IMMEDIATE COMFORT, NOT THE CHANGE YOU REQUESTED.",
        ),
    )
}
