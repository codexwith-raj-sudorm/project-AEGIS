package com.jarvis.aegis.challenge

import com.jarvis.aegis.session.ChallengeDifficulty
import java.security.SecureRandom
import kotlin.math.max

class ChallengeGenerator(private val random: SecureRandom = SecureRandom()) {
    private val recent = ArrayDeque<String>()

    fun generate(subjects: Set<String>, difficulty: ChallengeDifficulty): NumericChallenge {
        val eligible = buildList {
            if (subjects.any { it.contains("math", true) }) addAll(listOf(::arithmetic, ::algebra, ::logic, ::matrix))
            if (subjects.any { it.contains("phys", true) }) addAll(listOf(::kinematics, ::unitConversion))
            if (subjects.any { it.contains("chem", true) }) add(::chemistry)
            if (isEmpty()) addAll(listOf(::arithmetic, ::algebra, ::logic))
        }
        repeat(20) {
            val challenge = eligible[random.nextInt(eligible.size)](difficulty)
            if (challenge.signature !in recent) return remember(challenge)
        }
        return remember(arithmetic(difficulty))
    }

    fun arithmetic(difficulty: ChallengeDifficulty = ChallengeDifficulty.INTERMEDIATE): NumericChallenge {
        val scale = difficulty.scale
        val left = random.nextInt(40 * scale) + 10
        val right = random.nextInt(20 * scale) + 5
        val multiplier = random.nextInt(4 * scale) + 2
        return numeric(ChallengeCategory.ARITHMETIC, "Calculate: ($left + $right) × $multiplier", ((left + right) * multiplier).toDouble(), 15 + scale * 5)
    }

    fun algebra(difficulty: ChallengeDifficulty): NumericChallenge {
        val x = random.nextInt(8 * difficulty.scale) + 2
        val coefficient = random.nextInt(4 * difficulty.scale) + 2
        val offset = random.nextInt(12 * difficulty.scale) + 1
        val result = coefficient * x + offset
        return numeric(ChallengeCategory.ALGEBRA, "Solve for x: ${coefficient}x + $offset = $result", x.toDouble(), 20 + difficulty.scale * 6)
    }

    fun logic(difficulty: ChallengeDifficulty): NumericChallenge {
        val start = random.nextInt(12) + 1
        val step = random.nextInt(3 * difficulty.scale) + 2
        val terms = List(4) { start + it * step }
        return numeric(ChallengeCategory.LOGIC, "Find the next term: ${terms.joinToString(", ")}, ?", (start + 4 * step).toDouble(), 18 + difficulty.scale * 4)
    }

    fun kinematics(difficulty: ChallengeDifficulty = ChallengeDifficulty.INTERMEDIATE): NumericChallenge {
        val initial = random.nextInt(8 * difficulty.scale) + 5
        val acceleration = random.nextInt(4 * difficulty.scale) + 2
        val time = random.nextInt(3 * difficulty.scale) + 2
        return numeric(
            ChallengeCategory.KINEMATICS,
            "Find final velocity: u = $initial m/s, a = $acceleration m/s², t = $time s",
            (initial + acceleration * time).toDouble(), max(30, 22 + difficulty.scale * 8), "m/s",
        )
    }

    fun chemistry(difficulty: ChallengeDifficulty): NumericChallenge {
        val moles = random.nextInt(4 * difficulty.scale) + 1
        val molarMass = listOf(18, 32, 44, 58)[random.nextInt(4)]
        return numeric(
            ChallengeCategory.CHEMISTRY,
            "Calculate mass for $moles mol with molar mass $molarMass g/mol.",
            (moles * molarMass).toDouble(), 25 + difficulty.scale * 8, "g",
        )
    }

    fun unitConversion(difficulty: ChallengeDifficulty): NumericChallenge {
        val kilometres = random.nextInt(20 * difficulty.scale) + 1
        return numeric(
            ChallengeCategory.UNIT_CONVERSION,
            "Convert $kilometres km to metres.", kilometres * 1000.0,
            18 + difficulty.scale * 4, "m",
        )
    }

    fun matrix(difficulty: ChallengeDifficulty): NumericChallenge {
        val a = random.nextInt(8 * difficulty.scale) + 1
        val b = random.nextInt(8 * difficulty.scale) + 1
        val c = random.nextInt(8 * difficulty.scale) + 1
        val d = random.nextInt(8 * difficulty.scale) + 1
        return numeric(
            ChallengeCategory.MATRIX,
            "Find det([[${a}, ${b}], [${c}, ${d}]]).",
            (a * d - b * c).toDouble(), 28 + difficulty.scale * 8,
        )
    }

    private fun numeric(category: ChallengeCategory, prompt: String, answer: Double, seconds: Int, unit: String? = null) =
        NumericChallenge(category = category, prompt = prompt, expected = answer, timeLimitSeconds = seconds, unit = unit)

    private fun remember(challenge: NumericChallenge): NumericChallenge {
        recent.addLast(challenge.signature)
        while (recent.size > 25) recent.removeFirst()
        return challenge
    }

    private val ChallengeDifficulty.scale: Int
        get() = when (this) {
            ChallengeDifficulty.FOUNDATION -> 1
            ChallengeDifficulty.INTERMEDIATE -> 2
            ChallengeDifficulty.ADVANCED -> 3
        }
}
