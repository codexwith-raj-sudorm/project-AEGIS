package com.jarvis.aegis.session

import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

data class ConsentRecord(
    val sessionId: UUID,
    val policyVersion: String,
    val policyHash: String,
    val confirmedAt: Instant,
    val deviceAuthenticated: Boolean,
)

object ConsentHasher {
    const val POLICY_VERSION = "aegis-extreme-v1"

    fun hash(policy: SessionPolicy): String {
        val canonical = buildString {
            append(policy.mode.name).append('|')
            append(policy.duration.seconds).append('|')
            append(policy.targetPackages.sorted().joinToString(",")).append('|')
            append(policy.essentialPackages.sorted().joinToString(",")).append('|')
            append(policy.exitDelay.seconds).append('|')
            append(policy.amnestyEnabled).append('|')
            append(policy.challengeSubjects.sorted().joinToString(",")).append('|')
            append(policy.difficulty.name)
        }
        return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
