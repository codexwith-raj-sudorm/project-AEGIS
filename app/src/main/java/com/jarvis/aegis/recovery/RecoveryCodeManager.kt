package com.jarvis.aegis.recovery

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class RecoveryEnrollment(val displayCode: String, val verifier: String)

/** Per-install recovery verifier. No universal developer credential exists. */
class RecoveryCodeManager(
    private val random: SecureRandom = SecureRandom(),
    private val iterations: Int = 210_000,
) {
    fun enroll(): RecoveryEnrollment {
        val bytes = ByteArray(15).also(random::nextBytes)
        val code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
            .chunked(5).joinToString("-")
        return RecoveryEnrollment(code, createVerifier(code))
    }

    fun verify(candidate: CharArray, encodedVerifier: String): Boolean = runCatching {
        val parts = encodedVerifier.split('$')
        require(parts.size == 4 && parts[0] == "aegis-pbkdf2-v1")
        val rounds = parts[1].toInt()
        val salt = Base64.getDecoder().decode(parts[2])
        val expected = Base64.getDecoder().decode(parts[3])
        val actual = derive(candidate, salt, rounds)
        MessageDigest.isEqual(expected, actual)
    }.getOrDefault(false).also { candidate.fill('\u0000') }

    private fun createVerifier(code: String): String {
        val salt = ByteArray(16).also(random::nextBytes)
        val derived = derive(code.toCharArray(), salt, iterations)
        return listOf(
            "aegis-pbkdf2-v1", iterations.toString(),
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(derived),
        ).joinToString("$")
    }

    private fun derive(secret: CharArray, salt: ByteArray, rounds: Int): ByteArray {
        require(rounds in 100_000..1_000_000)
        val spec = PBEKeySpec(secret, salt, rounds, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword(); secret.fill('\u0000') }
    }
}
