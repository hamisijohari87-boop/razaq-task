package com.example.data.local

import java.security.MessageDigest
import kotlin.random.Random

object SecurityUtils {
    private const val SALT = "razak_task_tz_v1_salt"

    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$SALT:$password".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(rawPassword: String, storedHash: String): Boolean {
        return hashPassword(rawPassword) == storedHash
    }

    fun generateReferenceCode(prefix: String = "RZK"): String {
        val digits = Random.nextInt(100_000, 999_999)
        return "$prefix-$digits"
    }
}
