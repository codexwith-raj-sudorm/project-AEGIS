package com.jarvis.aegis.profile

enum class AgeBand(val label: String) {
    UNDER_13("Under 13"), TEEN("13–17"), ADULT("18+")
}

enum class MotivationProfile(val label: String) {
    NEUTRAL("Neutral"), REFLECTIVE("Reflective"), CHALLENGER("Challenger"), HARD_TRUTH("Hard Truth")
}

data class LearnerProfile(
    val displayName: String = "",
    val ageBand: AgeBand = AgeBand.ADULT,
    val grade: String = "Self-directed",
    val curriculum: String = "Custom",
    val subjects: Set<String> = setOf("Mathematics"),
    val motivation: MotivationProfile = MotivationProfile.REFLECTIVE,
) {
    val allowedMotivations: Set<MotivationProfile>
        get() = if (ageBand == AgeBand.ADULT) MotivationProfile.entries.toSet()
        else MotivationProfile.entries.toSet() - MotivationProfile.HARD_TRUTH
}
