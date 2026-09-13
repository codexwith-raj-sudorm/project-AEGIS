package com.jarvis.aegis.data

import android.content.Context
import com.jarvis.aegis.profile.AgeBand
import com.jarvis.aegis.profile.LearnerProfile
import com.jarvis.aegis.profile.MotivationProfile
import com.jarvis.aegis.session.FocusSession
import com.jarvis.aegis.session.SessionMode
import com.jarvis.aegis.session.SessionPolicy
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** Small, fail-open persistence layer. Encrypted storage migration is tracked for hardening. */
class AegisStore(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun saveProfile(profile: LearnerProfile) = preferences.edit()
        .putString(PROFILE, JSONObject().apply {
            put("name", profile.displayName)
            put("age", profile.ageBand.name)
            put("grade", profile.grade)
            put("curriculum", profile.curriculum)
            put("subjects", JSONArray(profile.subjects.toList()))
            put("motivation", profile.motivation.name)
        }.toString()).apply()

    fun profile(): LearnerProfile = runCatching {
        val json = JSONObject(preferences.getString(PROFILE, null) ?: return LearnerProfile())
        LearnerProfile(
            displayName = json.optString("name"),
            ageBand = AgeBand.valueOf(json.optString("age", AgeBand.ADULT.name)),
            grade = json.optString("grade", "Self-directed"),
            curriculum = json.optString("curriculum", "Custom"),
            subjects = json.optJSONArray("subjects")?.toStringSet() ?: setOf("Mathematics"),
            motivation = MotivationProfile.valueOf(json.optString("motivation", MotivationProfile.REFLECTIVE.name)),
        ).let { it.copy(motivation = it.motivation.takeIf(it.allowedMotivations::contains) ?: MotivationProfile.REFLECTIVE) }
    }.getOrElse { LearnerProfile() }

    fun saveSession(session: FocusSession) = preferences.edit()
        .putString(SESSION, JSONObject().apply {
            put("id", session.id.toString())
            put("mode", session.policy.mode.name)
            put("activatedAt", session.activatedAt.toString())
            put("expiresAt", session.expiresAt.toString())
            put("targets", JSONArray(session.policy.targetPackages.toList()))
            put("essential", JSONArray(session.policy.essentialPackages.toList()))
            put("exitDelaySeconds", session.policy.exitDelay.seconds)
            put("amnesty", session.policy.amnestyEnabled)
            put("streak", session.streak)
            put("interrupted", session.interrupted)
        }.toString()).apply()

    /** Any malformed, expired, or impossible state returns null and clears enforcement. */
    fun activeSession(now: Instant = Instant.now()): FocusSession? = runCatching {
        val raw = preferences.getString(SESSION, null) ?: return null
        val json = JSONObject(raw)
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
        if (tokens != null) preferences.edit().putInt(TOKENS, tokens.coerceAtLeast(0)).apply()
    }

    fun tokens(): Int = preferences.getInt(TOKENS, 0).coerceAtLeast(0)

    fun spendToken(): Boolean {
        val count = tokens()
        if (count <= 0) return false
        preferences.edit().putInt(TOKENS, count - 1).apply()
        return true
    }

    fun clearSession() {
        preferences.edit()
            .remove(SESSION)
            .remove(RECOVERY_VERIFIER)
            .remove(EXIT_REQUESTED_AT)
            .remove(EXIT_AVAILABLE_AT)
            .apply()
    }

    fun saveRecoveryVerifier(verifier: String) {
        preferences.edit().putString(RECOVERY_VERIFIER, verifier).apply()
    }

    fun recoveryVerifier(): String? = preferences.getString(RECOVERY_VERIFIER, null)

    fun saveExitRequest(requestedAt: Instant, availableAt: Instant) {
        preferences.edit()
            .putLong(EXIT_REQUESTED_AT, requestedAt.toEpochMilli())
            .putLong(EXIT_AVAILABLE_AT, availableAt.toEpochMilli())
            .apply()
    }

    fun exitAvailableAt(): Instant? = preferences.getLong(EXIT_AVAILABLE_AT, 0L)
        .takeIf { it > 0L }?.let(Instant::ofEpochMilli)

    fun cancelExitRequest() {
        preferences.edit().remove(EXIT_REQUESTED_AT).remove(EXIT_AVAILABLE_AT).apply()
    }

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
        const val RECOVERY_VERIFIER = "recovery_verifier"
        const val EXIT_REQUESTED_AT = "exit_requested_at"
        const val EXIT_AVAILABLE_AT = "exit_available_at"
        const val TOKENS = "sincerity_tokens"
    }
}
