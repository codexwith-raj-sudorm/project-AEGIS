package com.jarvis.aegis.challenge

import java.security.SecureRandom
import kotlin.math.max

class ChallengeGenerator(private val random: SecureRandom = SecureRandom()) {
    fun arithmetic(): NumericChallenge {
        val left = random.nextInt(81) + 10
        val right = random.nextInt(41) + 5
        val multiplier = random.nextInt(8) + 2
        return NumericChallenge(
            prompt = "Calculate: ($left + $right) × $multiplier",
            timeLimitSeconds = 25,
            expected = ((left + right) * multiplier).toDouble(),
        )
    }

    fun kinematics(): NumericChallenge {
        val initial = random.nextInt(16) + 5
        val acceleration = random.nextInt(8) + 2
        val time = random.nextInt(7) + 2
        return NumericChallenge(
            prompt = "Find final velocity: u = $initial m/s, a = $acceleration m/s², t = $time s",
            timeLimitSeconds = max(30, 20 + time),
            expected = (initial + acceleration * time).toDouble(),
            unit = "m/s",
        )
    }
}
