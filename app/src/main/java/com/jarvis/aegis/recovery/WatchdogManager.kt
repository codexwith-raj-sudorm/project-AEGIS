package com.jarvis.aegis.recovery

import android.content.Context
import java.time.Duration
import java.time.Instant

/**
 * Safety state is intentionally independent of encrypted session state: inability to decrypt
 * must never prevent fail-open recovery.
 */
class WatchdogManager(context: Context) {
    private val state = context.getSharedPreferences("aegis_safety_watchdog_v1", Context.MODE_PRIVATE)

    fun beginGateLaunch(now: Instant = Instant.now()): Boolean {
        val previousPending = state.getBoolean(PENDING, false)
        val windowStart = Instant.ofEpochMilli(state.getLong(WINDOW_START, now.toEpochMilli()))
        var failures = state.getInt(FAILURES, 0)
        val effectiveStart = if (Duration.between(windowStart, now) > WINDOW) {
            failures = 0
            now
        } else windowStart
        if (previousPending) failures++
        if (failures >= MAX_FAILURES) {
            state.edit().clear().putLong(FAIL_OPEN_UNTIL, now.plus(FAIL_OPEN_DURATION).toEpochMilli()).apply()
            return false
        }
        state.edit()
            .putBoolean(PENDING, true)
            .putInt(FAILURES, failures)
            .putLong(WINDOW_START, effectiveStart.toEpochMilli())
            .apply()
        return true
    }

    fun markGateHealthy() { state.edit().putBoolean(PENDING, false).apply() }

    fun isFailOpen(now: Instant = Instant.now()): Boolean {
        val until = state.getLong(FAIL_OPEN_UNTIL, 0L)
        if (until <= now.toEpochMilli()) {
            if (until != 0L) state.edit().clear().apply()
            return false
        }
        return true
    }

    companion object {
        private const val PENDING = "pending_gate_launch"
        private const val FAILURES = "consecutive_failures"
        private const val WINDOW_START = "window_start"
        private const val FAIL_OPEN_UNTIL = "fail_open_until"
        private const val MAX_FAILURES = 3
        private val WINDOW = Duration.ofSeconds(120)
        private val FAIL_OPEN_DURATION = Duration.ofMinutes(15)
    }
}
