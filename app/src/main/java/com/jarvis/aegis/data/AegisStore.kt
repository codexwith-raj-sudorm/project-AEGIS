package com.jarvis.aegis.data

import android.content.Context
import com.jarvis.aegis.profile.AgeBand
import com.jarvis.aegis.profile.LearnerProfile
import com.jarvis.aegis.profile.MotivationProfile
import com.jarvis.aegis.recovery.AttemptState
import com.jarvis.aegis.security.SecurePreferences
import com.jarvis.aegis.session.ChallengeDifficulty
import com.jarvis.aegis.session.ConsentRecord
import com.jarvis.aegis.session.FocusSession
import com.jarvis.aegis.session.SessionMode
import com.jarvis.aegis.session.SessionPolicy
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** Keystore-encrypted state with fail-open parsing and legacy plaintext migration. */
class AegisStore(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val secure = SecurePreferences(preferences)

    fun saveProfile(profile: LearnerProfile) = secure.putString(PROFILE, JSONObject().apply {
        put("name", profile.displayName)
        put("age", profile.ageBand.name)
        put("grade", profile.grade)
        put("curriculum", profile.curriculum)
        put("subjects", JSONArray(profile.subjects.toList()))
        put("motivation", profile.motivation.name)
    }.toString())

    fun profile(): LearnerProfile = runCatching {
        val json = JSONObject(secure.getString(PROFILE) ?: return LearnerProfile())
        LearnerProfile(
            displayName = json.optString("name"),
            ageBand = AgeBand.valueOf(json.optString("age", AgeBand.ADULT.name)),
            grade = json.optString("grade", "Self-directed"),
            curriculum = json.optString("curriculum", "Custom"),
            subjects = json.optJSONArray("subjects")?.toStringSet() ?: setOf("Mathematics"),
            motivation = MotivationProfile.valueOf(json.optString("motivation", MotivationProfile.REFLECTIVE.name)),
        ).let { it.copy(motivation = it.motivation.takeIf(it.allowedMotivations::contains) ?: MotivationProfile.REFLECTIVE) }
    }.getOrElse { LearnerProfile() }

    fun saveConsent(record: ConsentRecord) = secure.putString(CONSENT, JSONObject().apply {
        put("sessionId", record.sessionId.toString())
        put("policyVersion", record.policyVersion)
        put("policyHash", record.policyHash)
        put("confirmedAt", record.confirmedAt.toString())
        put("deviceAuthenticated", record.deviceAuthenticated)
    }.toString())

    fun saveSession(session: FocusSession) = secure.putString(SESSION, JSONObject().apply {
        put("id", session.id.toString())
        put("mode", session.policy.mode.name)
        put("activatedAt", session.activatedAt.toString())
        put("expiresAt", session.expiresAt.toString())
        put("targets", JSONArray(session.policy.targetPackages.toList()))
        put("essential", JSONArray(session.policy.essentialPackages.toList()))
        put("exitDelaySeconds", session.policy.exitDelay.seconds)
        put("amnesty", session.policy.amnestyEnabled)
        put("challengeSubjects", JSONArray(session.policy.challengeSubjects.toList()))
        put("difficulty", session.policy.difficulty.name)
        put("streak", session.streak)
        put("interrupted", session.interrupted)
    }.toString())

    /** Any undecryptable, malformed, expired, or impossible session fails open. */
    fun activeSession(now: Instant = Instant.now()): FocusSession? = runCatching {
        val json = JSONObject(secure.getString(SESSION) ?: return null)
        val activated = Instant.parse(json.getString("activatedAt"))
        val expires = Instant.parse(json.getString("expiresAt"))
        if (!now.isBefore(expires) || expires.isBefore(activated)) return clearSession().let { null }
        val policy = SessionPolicy(
            mode = SessionMode.valueOf(json.getString("mode")),
            duration = Duration.between(activated, expires),
            targetPackages = json.getJSONArray("targets").toStringSet(),
            essentialPackages = json.getJSONArray("essential").toStringSet(),
            exitDelay = Duration.ofSeconds(json.optLong("exitDelaySeconds", 300)),
            amnestyEnabled = json.optBoolean("amnesty", true),
            challengeSubjects = json.optJSONArray("challengeSubjects")?.toStringSet() ?: setOf("Mathematics"),
            difficulty = ChallengeDifficulty.valueOf(json.optString("difficulty", ChallengeDifficulty.INTERMEDIATE.name)),
        )
        FocusSession(
            id = UUID.fromString(json.getString("id")), policy = policy,
            activatedAt = activated, expiresAt = expires,
            streak = json.optInt("streak"), interrupted = json.optBoolean("interrupted"),
        ).takeIf { it.isActive(now) }
    }.getOrElse { clearSession(); null }

    fun updateProgress(streak: Int, tokens: Int? = null) {
        val session = activeSession() ?: return
        saveSession(session.copy(streak = streak.coerceAtLeast(0)))
        tokens?.let { secure.putInt(TOKENS, it.coerceAtLeast(0)) }
    }

    fun tokens(): Int = secure.getInt(TOKENS).coerceAtLeast(0)

    fun spendToken(): Boolean {
        val count = tokens()
        if (count <= 0) return false
        secure.putInt(TOKENS, count - 1)
        return true
    }

    fun clearSession() = secure.remove(SESSION, CONSENT, RECOVERY_VERIFIER, EXIT_REQUESTED_AT, EXIT_AVAILABLE_AT, RECOVERY_ATTEMPTS)
    fun saveRecoveryVerifier(verifier: String) = secure.putString(RECOVERY_VERIFIER, verifier)
    fun recoveryVerifier(): String? = secure.getString(RECOVERY_VERIFIER)

    fun saveExitRequest(requestedAt: Instant, availableAt: Instant) {
        secure.putLong(EXIT_REQUESTED_AT, requestedAt.toEpochMilli())
        secure.putLong(EXIT_AVAILABLE_AT, availableAt.toEpochMilli())
    }

    fun exitAvailableAt(): Instant? = secure.getLong(EXIT_AVAILABLE_AT)
        .takeIf { it > 0L }?.let(Instant::ofEpochMilli)

    fun cancelExitRequest() = secure.remove(EXIT_REQUESTED_AT, EXIT_AVAILABLE_AT)

    fun recoveryAttemptState(): AttemptState = runCatching {
        val json = JSONObject(secure.getString(RECOVERY_ATTEMPTS) ?: return AttemptState())
        AttemptState(
            failures = json.optInt("failures").coerceAtLeast(0),
            blockedUntil = json.optString("blockedUntil").takeIf(String::isNotBlank)?.let(Instant::parse),
        )
    }.getOrDefault(AttemptState())

    fun saveRecoveryAttemptState(state: AttemptState) = secure.putString(
        RECOVERY_ATTEMPTS,
        JSONObject().apply {
            put("failures", state.failures)
            put("blockedUntil", state.blockedUntil?.toString() ?: "")
        }.toString(),
    )

    // Access grants are short-lived enforcement metadata rather than sensitive profile content.
    fun grantTarget(packageName: String, until: Instant) = preferences.edit()
        .putLong("grant:$packageName", until.toEpochMilli()).apply()

    fun isGranted(packageName: String, now: Instant = Instant.now()): Boolean =
        preferences.getLong("grant:$packageName", 0) > now.toEpochMilli()

    private fun JSONArray.toStringSet() = buildSet {
        for (index in 0 until length()) add(getString(index))
    }

    private companion object {
        const val NAME = "aegis_state_v1"
        const val PROFILE = "profile"
        const val SESSION = "session"
        const val CONSENT = "consent_record"
        const val RECOVERY_VERIFIER = "recovery_verifier"
        const val EXIT_REQUESTED_AT = "exit_requested_at"
        const val EXIT_AVAILABLE_AT = "exit_available_at"
        const val TOKENS = "sincerity_tokens"
        const val RECOVERY_ATTEMPTS = "recovery_attempts"
    }
}
