package com.jarvis.aegis.data

import android.content.Context
import com.jarvis.aegis.challenge.ActiveChallenge
import com.jarvis.aegis.challenge.ChallengeCategory
import com.jarvis.aegis.challenge.GateTransaction
import com.jarvis.aegis.challenge.NumericChallenge
import com.jarvis.aegis.profile.AgeBand
import com.jarvis.aegis.profile.LearnerProfile
import com.jarvis.aegis.profile.MotivationProfile
import com.jarvis.aegis.recovery.AttemptState
import com.jarvis.aegis.security.SecurePreferences
import com.jarvis.aegis.session.ChallengeDifficulty
import com.jarvis.aegis.session.ConsentRecord
import com.jarvis.aegis.session.DeadlineState
import com.jarvis.aegis.session.DeviceTimeSource
import com.jarvis.aegis.session.FocusSession
import com.jarvis.aegis.session.LaunchAttemptState
import com.jarvis.aegis.session.SessionMode
import com.jarvis.aegis.session.SessionPolicy
import com.jarvis.aegis.session.TimeAnchor
import com.jarvis.aegis.session.TimeIntegrity
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** Keystore-encrypted state with fail-open parsing and legacy plaintext migration. */
class AegisStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val secure = SecurePreferences(preferences)

    fun recordForegroundPackage(packageName: String) = secure.putString(LAST_FOREGROUND, packageName)
    fun lastForegroundPackage(): String? = secure.getString(LAST_FOREGROUND)

    fun clockSnapshot() = DeviceTimeSource(appContext).snapshot()
    fun timeAnchor() = DeviceTimeSource(appContext).anchor()

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
        session.timeAnchor?.let { anchor ->
            put("anchorWall", anchor.wallTime.toString())
            put("anchorElapsed", anchor.elapsedRealtimeMs)
            put("anchorBoot", anchor.bootCount)
        }
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
        val anchor = json.optString("anchorWall").takeIf(String::isNotBlank)?.let {
            TimeAnchor(
                wallTime = Instant.parse(it),
                elapsedRealtimeMs = json.getLong("anchorElapsed"),
                bootCount = json.getInt("anchorBoot"),
            )
        }
        FocusSession(
            id = UUID.fromString(json.getString("id")), policy = policy,
            activatedAt = activated, expiresAt = expires,
            streak = json.optInt("streak"), interrupted = json.optBoolean("interrupted"),
            timeAnchor = anchor,
        ).takeIf { session ->
            val state = session.deadlineState(DeviceTimeSource(appContext).snapshot())
            state == DeadlineState.ACTIVE
        } ?: clearSession().let { null }
    }.getOrElse { clearSession(); null }

    fun saveActiveChallenge(active: ActiveChallenge) = secure.putString(ACTIVE_CHALLENGE, JSONObject().apply {
        put("sessionId", active.sessionId.toString())
        put("target", active.targetPackage)
        put("issuedAt", active.issuedAt.toString())
        put("deadline", active.deadline.toString())
        put("id", active.challenge.id.toString())
        put("category", active.challenge.category.name)
        put("prompt", active.challenge.prompt)
        put("seconds", active.challenge.timeLimitSeconds)
        put("signature", active.challenge.signature)
        put("expected", active.challenge.expected)
        put("unit", active.challenge.unit ?: "")
        put("absoluteTolerance", active.challenge.absoluteTolerance)
        put("relativeTolerance", active.challenge.relativeTolerance)
        active.timeAnchor?.let { anchor ->
            put("anchorWall", anchor.wallTime.toString())
            put("anchorElapsed", anchor.elapsedRealtimeMs)
            put("anchorBoot", anchor.bootCount)
        }
    }.toString())

    fun activeChallenge(): ActiveChallenge? = runCatching {
        val json = JSONObject(secure.getString(ACTIVE_CHALLENGE) ?: return null)
        ActiveChallenge(
            sessionId = UUID.fromString(json.getString("sessionId")),
            targetPackage = json.getString("target"),
            issuedAt = Instant.parse(json.getString("issuedAt")),
            deadline = Instant.parse(json.getString("deadline")),
            timeAnchor = json.optString("anchorWall").takeIf(String::isNotBlank)?.let {
                TimeAnchor(Instant.parse(it), json.getLong("anchorElapsed"), json.getInt("anchorBoot"))
            },
            challenge = NumericChallenge(
                id = UUID.fromString(json.getString("id")),
                category = ChallengeCategory.valueOf(json.getString("category")),
                prompt = json.getString("prompt"),
                timeLimitSeconds = json.getInt("seconds"),
                signature = json.getString("signature"),
                expected = json.getDouble("expected"),
                unit = json.optString("unit").takeIf(String::isNotBlank),
                absoluteTolerance = json.getDouble("absoluteTolerance"),
                relativeTolerance = json.getDouble("relativeTolerance"),
            ),
        )
    }.getOrElse { secure.remove(ACTIVE_CHALLENGE); null }

    /** Returns true exactly once for the currently active challenge ID. */
    fun consumeActiveChallenge(challengeId: UUID): Boolean = synchronized(CHALLENGE_LOCK) {
        val current = activeChallenge() ?: return@synchronized false
        if (current.challenge.id != challengeId) return@synchronized false
        secure.remove(ACTIVE_CHALLENGE)
        true
    }

    fun clearActiveChallenge() = secure.remove(ACTIVE_CHALLENGE)

    fun saveGateTransaction(transaction: GateTransaction) = secure.putString(GATE_JOURNAL, JSONObject().apply {
        put("id", transaction.id.toString())
        put("challengeId", transaction.challengeId.toString())
        put("sessionId", transaction.sessionId.toString())
        put("target", transaction.target)
        put("finalStreak", transaction.finalStreak)
        put("finalTokens", transaction.finalTokens)
        put("grantUntil", transaction.grantUntil.toString())
        put("spendToken", transaction.spendToken)
    }.toString())

    fun wasChallengeCompleted(id: UUID): Boolean = completedChallengeIds().contains(id.toString())

    fun recoverPendingGateTransaction() {
        val raw = secure.getString(GATE_JOURNAL) ?: return
        runCatching { applyGateTransaction(parseTransaction(JSONObject(raw))) }
            .onFailure {
                // A malformed journal cannot safely be applied. Disarm the session rather than guess.
                clearSession()
                secure.remove(GATE_JOURNAL)
            }
    }

    fun applyGateTransaction(transaction: GateTransaction) = synchronized(CHALLENGE_LOCK) {
        if (wasChallengeCompleted(transaction.challengeId)) {
            secure.remove(GATE_JOURNAL)
            return@synchronized
        }
        val session = activeSession()
        if (session == null || session.id != transaction.sessionId) {
            secure.remove(GATE_JOURNAL)
            return@synchronized
        }
        secure.remove(ACTIVE_CHALLENGE)
        saveSession(session.copy(streak = transaction.finalStreak.coerceAtLeast(0)))
        secure.putInt(TOKENS, transaction.finalTokens.coerceAtLeast(0))
        grantTarget(transaction.target, transaction.grantUntil)
        val completed = (completedChallengeIds() + transaction.challengeId.toString()).takeLast(64)
        secure.putString(COMPLETED_CHALLENGES, JSONArray(completed).toString())
        secure.remove(GATE_JOURNAL)
    }

    private fun completedChallengeIds(): List<String> = runCatching {
        val array = JSONArray(secure.getString(COMPLETED_CHALLENGES) ?: "[]")
        List(array.length()) { array.getString(it) }
    }.getOrDefault(emptyList())

    private fun parseTransaction(json: JSONObject) = GateTransaction(
        id = UUID.fromString(json.getString("id")),
        challengeId = UUID.fromString(json.getString("challengeId")),
        sessionId = UUID.fromString(json.getString("sessionId")),
        target = json.getString("target"),
        finalStreak = json.getInt("finalStreak"),
        finalTokens = json.getInt("finalTokens"),
        grantUntil = Instant.parse(json.getString("grantUntil")),
        spendToken = json.getBoolean("spendToken"),
    )

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

    fun clearSession() = secure.remove(
        SESSION, CONSENT, ACTIVE_CHALLENGE, GATE_JOURNAL, COMPLETED_CHALLENGES,
        RECOVERY_VERIFIER, EXIT_REQUESTED_AT, EXIT_AVAILABLE_AT, EXIT_TIMER, RECOVERY_ATTEMPTS,
    )
    fun saveRecoveryVerifier(verifier: String) = secure.putString(RECOVERY_VERIFIER, verifier)
    fun recoveryVerifier(): String? = secure.getString(RECOVERY_VERIFIER)

    fun saveExitRequest(requestedAt: Instant, availableAt: Instant) {
        val snapshot = clockSnapshot().copy(wallTime = requestedAt)
        secure.putString(EXIT_TIMER, JSONObject().apply {
            put("wall", snapshot.wallTime.toString())
            put("elapsed", snapshot.elapsedRealtimeMs)
            put("boot", snapshot.bootCount)
            put("durationMs", Duration.between(requestedAt, availableAt).toMillis().coerceAtLeast(0))
        }.toString())
        secure.remove(EXIT_REQUESTED_AT, EXIT_AVAILABLE_AT)
    }

    fun exitAvailableAt(): Instant? = runCatching {
        val raw = secure.getString(EXIT_TIMER)
        if (raw == null) {
            return secure.getLong(EXIT_AVAILABLE_AT).takeIf { it > 0L }?.let(Instant::ofEpochMilli)
        }
        val json = JSONObject(raw)
        val anchor = TimeAnchor(Instant.parse(json.getString("wall")), json.getLong("elapsed"), json.getInt("boot"))
        val duration = Duration.ofMillis(json.getLong("durationMs"))
        val snapshot = clockSnapshot()
        when (TimeIntegrity.evaluate(anchor, duration, snapshot)) {
            DeadlineState.ACTIVE -> snapshot.wallTime.plus(TimeIntegrity.remaining(anchor, duration, snapshot)!!)
            DeadlineState.EXPIRED -> snapshot.wallTime
            DeadlineState.INVALID -> null.also { secure.remove(EXIT_TIMER) }
        }
    }.getOrElse { secure.remove(EXIT_TIMER); null }

    fun cancelExitRequest() = secure.remove(EXIT_REQUESTED_AT, EXIT_AVAILABLE_AT, EXIT_TIMER)

    fun recoveryAttemptState(): AttemptState = runCatching {
        val json = JSONObject(secure.getString(RECOVERY_ATTEMPTS) ?: return AttemptState())
        val snapshot = clockSnapshot()
        val blockedUntil = if (json.has("anchorWall") && json.optLong("durationMs") > 0) {
            val anchor = TimeAnchor(Instant.parse(json.getString("anchorWall")), json.getLong("anchorElapsed"), json.getInt("anchorBoot"))
            val duration = Duration.ofMillis(json.getLong("durationMs"))
            TimeIntegrity.remaining(anchor, duration, snapshot)?.let { snapshot.wallTime.plus(it) }
        } else json.optString("blockedUntil").takeIf(String::isNotBlank)?.let(Instant::parse)
        AttemptState(json.optInt("failures").coerceAtLeast(0), blockedUntil)
    }.getOrElse { AttemptState() }

    fun saveRecoveryAttemptState(state: AttemptState) {
        val snapshot = clockSnapshot()
        val duration = state.blockedUntil?.let { Duration.between(snapshot.wallTime, it).coerceAtLeast(Duration.ZERO) } ?: Duration.ZERO
        secure.putString(RECOVERY_ATTEMPTS, JSONObject().apply {
            put("failures", state.failures)
            put("blockedUntil", state.blockedUntil?.toString() ?: "")
            put("anchorWall", snapshot.wallTime.toString())
            put("anchorElapsed", snapshot.elapsedRealtimeMs)
            put("anchorBoot", snapshot.bootCount)
            put("durationMs", duration.toMillis())
        }.toString())
    }

    fun launchAttemptState(packageName: String): LaunchAttemptState = runCatching {
        val json = JSONObject(secure.getString("$LAUNCH_ATTEMPT_PREFIX$packageName") ?: return LaunchAttemptState())
        val snapshot = clockSnapshot()
        val cooldownUntil = if (json.has("anchorWall")) {
            val anchor = TimeAnchor(Instant.parse(json.getString("anchorWall")), json.getLong("anchorElapsed"), json.getInt("anchorBoot"))
            val duration = Duration.ofMillis(json.getLong("cooldownDurationMs"))
            TimeIntegrity.remaining(anchor, duration, snapshot)?.let { snapshot.wallTime.plus(it) } ?: Instant.EPOCH
        } else Instant.parse(json.getString("cooldownUntil"))
        LaunchAttemptState(
            attempts = json.optInt("attempts").coerceAtLeast(0),
            windowStartedAt = Instant.parse(json.getString("windowStartedAt")),
            cooldownUntil = cooldownUntil,
        )
    }.getOrElse { resetLaunchAttempts(packageName); LaunchAttemptState() }

    fun resetLaunchAttempts(packageName: String) = secure.remove("$LAUNCH_ATTEMPT_PREFIX$packageName")

    fun saveLaunchAttemptState(packageName: String, state: LaunchAttemptState) {
        val snapshot = clockSnapshot()
        secure.putString(
            "$LAUNCH_ATTEMPT_PREFIX$packageName",
            JSONObject().apply {
                put("attempts", state.attempts)
                put("windowStartedAt", state.windowStartedAt.toString())
                put("cooldownUntil", state.cooldownUntil.toString())
                put("anchorWall", snapshot.wallTime.toString())
                put("anchorElapsed", snapshot.elapsedRealtimeMs)
                put("anchorBoot", snapshot.bootCount)
                put("cooldownDurationMs", Duration.between(snapshot.wallTime, state.cooldownUntil).toMillis().coerceAtLeast(0))
            }.toString(),
        )
    }

    fun grantTarget(packageName: String, until: Instant) {
        val snapshot = clockSnapshot()
        val duration = Duration.between(snapshot.wallTime, until).coerceAtLeast(Duration.ZERO)
        secure.putString("grant:$packageName", JSONObject().apply {
            put("wall", snapshot.wallTime.toString())
            put("elapsed", snapshot.elapsedRealtimeMs)
            put("boot", snapshot.bootCount)
            put("durationMs", duration.toMillis())
        }.toString())
    }

    fun isGranted(packageName: String, now: Instant = Instant.now()): Boolean = runCatching {
        val json = JSONObject(secure.getString("grant:$packageName") ?: return false)
        val anchor = TimeAnchor(Instant.parse(json.getString("wall")), json.getLong("elapsed"), json.getInt("boot"))
        val duration = Duration.ofMillis(json.getLong("durationMs"))
        TimeIntegrity.evaluate(anchor, duration, clockSnapshot().copy(wallTime = now)) == DeadlineState.ACTIVE
    }.getOrElse { secure.remove("grant:$packageName"); false }

    private fun JSONArray.toStringSet() = buildSet {
        for (index in 0 until length()) add(getString(index))
    }

    private companion object {
        val CHALLENGE_LOCK = Any()
        const val NAME = "aegis_state_v1"
        const val PROFILE = "profile"
        const val SESSION = "session"
        const val CONSENT = "consent_record"
        const val ACTIVE_CHALLENGE = "active_challenge"
        const val GATE_JOURNAL = "gate_transaction_journal"
        const val COMPLETED_CHALLENGES = "completed_challenge_ids"
        const val RECOVERY_VERIFIER = "recovery_verifier"
        const val EXIT_REQUESTED_AT = "exit_requested_at"
        const val EXIT_AVAILABLE_AT = "exit_available_at"
        const val EXIT_TIMER = "exit_timer"
        const val TOKENS = "sincerity_tokens"
        const val RECOVERY_ATTEMPTS = "recovery_attempts"
        const val LAUNCH_ATTEMPT_PREFIX = "launch_attempt:"
        const val LAST_FOREGROUND = "last_foreground"
    }
}
