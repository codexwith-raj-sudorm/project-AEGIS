package com.jarvis.aegis.security

import android.content.SharedPreferences

/** Encrypts values and migrates legacy plaintext values after a successful read. */
class SecurePreferences(
    private val preferences: SharedPreferences,
    private val cipher: KeystoreCipher = KeystoreCipher(),
) {
    fun putString(key: String, value: String) {
        preferences.edit().putString(key, PREFIX + cipher.encrypt(value)).apply()
    }

    fun getString(key: String): String? {
        val stored = preferences.getString(key, null) ?: return null
        if (stored.startsWith(PREFIX)) {
            return runCatching { cipher.decrypt(stored.removePrefix(PREFIX)) }.getOrNull()
        }
        // One-time migration from the pre-encryption prototype.
        putString(key, stored)
        return stored
    }

    fun putLong(key: String, value: Long) = putString(key, value.toString())
    fun getLong(key: String, default: Long = 0): Long = getString(key)?.toLongOrNull() ?: default
    fun putInt(key: String, value: Int) = putString(key, value.toString())
    fun getInt(key: String, default: Int = 0): Int = getString(key)?.toIntOrNull() ?: default

    fun remove(vararg keys: String) {
        preferences.edit().apply { keys.forEach(::remove) }.apply()
    }

    companion object { private const val PREFIX = "enc:v1:" }
}
