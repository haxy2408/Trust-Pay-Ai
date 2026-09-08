package com.example.trustpay.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * DEMO Bank OTP Verification Module.
 *
 * Requirements:
 * - MUST be clearly labeled: "DEMO BANK OTP — NOT A REAL BANK SERVICE".
 * - Generates 6-digit demo OTP.
 * - Displays OTP in development/demo environment for easy testing.
 * - OTP expires after a short period (e.g. 60 seconds).
 * - Limits number of incorrect attempts (max 3).
 * - Generates a new OTP after expiration.
 * - Never stores OTPs as plaintext in production storage (hashed in memory).
 * - Does not connect to real bank or real banking OTP system.
 * - Clearly indicates simulated authentication.
 */
object DemoBankOtp {

    const val DEMO_LABEL = "DEMO BANK OTP — NOT A REAL BANK SERVICE"
    private const val OTP_TTL_SECONDS = 60
    private const val MAX_ATTEMPTS = 3

    data class OtpSession(
        val sessionId: String,
        val hashedOtp: String,
        val demoPlainOtpForDisplay: String, // Exposed ONLY for educational/demo purposes
        val generatedTimestamp: Long,
        val expiresAtTimestamp: Long,
        var failedAttempts: Int = 0,
        var isConsumed: Boolean = false
    )

    data class OtpVerificationResult(
        val otpVerified: Boolean,
        val authenticationMethod: String = "DEMO_BANK_OTP",
        val riskLevel: String, // "LOW" or "HIGH"
        val attemptsRemaining: Int,
        val isExpired: Boolean,
        val message: String
    )

    private val random = SecureRandom()
    private var activeOtpSession: OtpSession? = null

    fun generateNewOtp(): OtpSession {
        val plainOtp = String.format("%06d", random.nextInt(1_000_000))
        val now = System.currentTimeMillis()
        val expiresAt = now + (OTP_TTL_SECONDS * 1000L)
        val hashed = hashOtp(plainOtp)

        val session = OtpSession(
            sessionId = "OTP-" + System.currentTimeMillis().toString().takeLast(6),
            hashedOtp = hashed,
            demoPlainOtpForDisplay = plainOtp,
            generatedTimestamp = now,
            expiresAtTimestamp = expiresAt
        )
        activeOtpSession = session
        return session
    }

    fun getActiveSession(): OtpSession {
        val current = activeOtpSession
        if (current == null || isExpired(current) || current.isConsumed) {
            return generateNewOtp()
        }
        return current
    }

    fun getRemainingSeconds(): Int {
        val session = activeOtpSession ?: return 0
        val remainingMs = session.expiresAtTimestamp - System.currentTimeMillis()
        return if (remainingMs > 0) (remainingMs / 1000).toInt() else 0
    }

    fun isExpired(session: OtpSession): Boolean {
        return System.currentTimeMillis() > session.expiresAtTimestamp
    }

    fun verifyOtp(enteredOtp: String): OtpVerificationResult {
        val session = activeOtpSession
            ?: return OtpVerificationResult(
                otpVerified = false,
                riskLevel = "HIGH",
                attemptsRemaining = 0,
                isExpired = true,
                message = "No active OTP session found. Please generate a new OTP."
            )

        if (isExpired(session)) {
            activeOtpSession = null
            return OtpVerificationResult(
                otpVerified = false,
                riskLevel = "HIGH",
                attemptsRemaining = 0,
                isExpired = true,
                message = "OTP expired. A fresh demo OTP has been generated."
            )
        }

        if (session.failedAttempts >= MAX_ATTEMPTS) {
            activeOtpSession = null
            return OtpVerificationResult(
                otpVerified = false,
                riskLevel = "HIGH",
                attemptsRemaining = 0,
                isExpired = false,
                message = "Maximum OTP verification attempts ($MAX_ATTEMPTS) exceeded. Transaction blocked."
            )
        }

        val enteredHashed = hashOtp(enteredOtp.trim())
        return if (enteredHashed == session.hashedOtp) {
            session.isConsumed = true
            OtpVerificationResult(
                otpVerified = true,
                riskLevel = "LOW",
                attemptsRemaining = MAX_ATTEMPTS - session.failedAttempts,
                isExpired = false,
                message = "Demo bank OTP verified successfully."
            )
        } else {
            session.failedAttempts++
            val remaining = MAX_ATTEMPTS - session.failedAttempts
            OtpVerificationResult(
                otpVerified = false,
                riskLevel = "HIGH",
                attemptsRemaining = remaining.coerceAtLeast(0),
                isExpired = false,
                message = if (remaining > 0) "Incorrect demo OTP. $remaining attempt(s) remaining." else "Max attempts reached. Transaction halted."
            )
        }
    }

    private fun hashOtp(otp: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(otp.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
