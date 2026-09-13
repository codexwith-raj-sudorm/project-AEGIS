package com.jarvis.aegis.session

enum class ExitPolicy { IMMEDIATE, DELAYED_OR_KEY, KEY_ONLY }

data class BlockingRules(
    val exitPolicy: ExitPolicy,
    val resetStreakOnContextSwitch: Boolean,
    val invalidateChallengeOnContextSwitch: Boolean,
    val cooldownMultiplier: Int,
    val requireDeviceAuthentication: Boolean,
    val allowAmnesty: Boolean,
)

val SessionMode.rules: BlockingRules
    get() = when (this) {
        SessionMode.STANDARD -> BlockingRules(
            exitPolicy = ExitPolicy.IMMEDIATE,
            resetStreakOnContextSwitch = false,
            invalidateChallengeOnContextSwitch = true,
            cooldownMultiplier = 0,
            requireDeviceAuthentication = false,
            allowAmnesty = true,
        )
        SessionMode.STRICT -> BlockingRules(
            exitPolicy = ExitPolicy.DELAYED_OR_KEY,
            resetStreakOnContextSwitch = true,
            invalidateChallengeOnContextSwitch = true,
            cooldownMultiplier = 1,
            requireDeviceAuthentication = true,
            allowAmnesty = true,
        )
        SessionMode.EXTREME -> BlockingRules(
            exitPolicy = ExitPolicy.KEY_ONLY,
            resetStreakOnContextSwitch = true,
            invalidateChallengeOnContextSwitch = true,
            cooldownMultiplier = 2,
            requireDeviceAuthentication = true,
            allowAmnesty = false,
        )
    }
