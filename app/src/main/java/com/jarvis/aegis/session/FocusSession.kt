package com.jarvis.aegis.session

import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class SessionMode { STANDARD, STRICT, EXTREME }
enum class ChallengeDifficulty { FOUNDATION, INTERMEDIATE, ADVANCED }

data class SessionPolicy(
    val mode: SessionMode,
    val duration: Duration,
    val targetPackages: Set<String>,
    val essentialPackages: Set<String>,
    val exitDelay: Duration = Duration.ofMinutes(5),
    val amnestyEnabled: Boolean = true,
    val challengeSubjects: Set<String> = setOf("Mathematics"),
    val difficulty: ChallengeDifficulty = ChallengeDifficulty.INTERMEDIATE,
) {
    init {
        require(!duration.isNegative && !duration.isZero) { "Duration must be positive" }
        require(mode != SessionMode.EXTREME || duration <= Duration.ofHours(24)) {
            "Extreme sessions are limited to 24 hours"
        }
        require(exitDelay in Duration.ofMinutes(1)..Duration.ofMinutes(10)) {
            "Exit delay must be between 1 and 10 minutes"
        }
        require(targetPackages.intersect(essentialPackages).isEmpty()) {
            "Essential packages cannot be targets"
        }
        require(targetPackages.isNotEmpty()) { "At least one target is required" }
        require(challengeSubjects.isNotEmpty()) { "At least one challenge subject is required" }
    }
}

data class FocusSession(
    val id: UUID = UUID.randomUUID(),
    val policy: SessionPolicy,
    val activatedAt: Instant,
    val expiresAt: Instant = activatedAt.plus(policy.duration),
    val streak: Int = 0,
    val interrupted: Boolean = false,
) {
    fun isActive(now: Instant): Boolean = !interrupted && now.isBefore(expiresAt)
}
