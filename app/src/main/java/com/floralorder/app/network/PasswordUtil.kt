package com.floralorder.app.network

import java.security.MessageDigest
import java.security.SecureRandom

/** Salted SHA-256 so passwords are never stored as plain text. */
object PasswordUtil {
    private const val SEPARATOR = ":"

    fun hash(password: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return toHex(salt) + SEPARATOR + sha256(salt, password)
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(SEPARATOR)
        if (parts.size != 2) return false
        return sha256(hexToBytes(parts[0]), password) == parts[1]
    }

    private fun sha256(salt: ByteArray, password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(password.toByteArray(Charsets.UTF_8))
        return toHex(digest.digest())
    }

    private fun toHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i -> hex.substring(2 * i, 2 * i + 2).toInt(16).toByte() }
}
