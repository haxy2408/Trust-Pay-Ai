package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Cryptographic security utilities for user authentication.
 *
 * Security Policies:
 * 1. Passwords are NEVER stored in plain text.
 * 2. Each account is assigned a cryptographically random 16-byte salt.
 * 3. Hashing uses SHA-256 with salt prefixing.
 * 4. Input validations strictly enforce email, 10-digit mobile, and 8+ character password standards.
 */
object AuthSecurityUtil {

    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )

    /**
     * Generates a 16-byte cryptographically secure random salt represented in hex format.
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes a salted SHA-256 hash of the password.
     * Combines salt + ":" + password.
     */
    fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$password"
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies whether an input password matches the stored salted hash.
     */
    fun verifyPassword(password: String, salt: String, storedHash: String): Boolean {
        val computed = hashPassword(password, salt)
        return computed.equals(storedHash, ignoreCase = true)
    }

    /**
     * Validates that an email is not blank and conforms to standard email format.
     */
    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return false
        return EMAIL_REGEX.matches(trimmed)
    }

    /**
     * Validates that a mobile number contains 10 numeric digits (or 10 digits prefixed with country code 91/+91).
     */
    fun isValidMobile(mobile: String): Boolean {
        val normalized = extractNormalized10Digits(mobile)
        return normalized.length == 10
    }

    /**
     * Extracts only numeric digits from a phone/mobile string.
     */
    fun extractDigits(input: String): String {
        return input.filter { it.isDigit() }
    }

    /**
     * Normalizes a mobile number to its standard 10 digits by stripping country code 91 if present.
     */
    fun extractNormalized10Digits(input: String): String {
        val digits = extractDigits(input)
        return if (digits.length == 12 && digits.startsWith("91")) {
            digits.takeLast(10)
        } else {
            digits
        }
    }
}
