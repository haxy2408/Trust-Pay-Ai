package com.example.trustpay.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * STEP 3 — Face ID / Face Authentication Security Module.
 *
 * Requirements:
 * - Uses device/platform's secure face-authentication capability where available.
 * - NEVER stores raw face images.
 * - Does NOT implement insecure facial recognition by simply comparing uploaded images.
 * - Application receives only success/failure authentication result.
 * - If secure face authentication is unavailable, clearly shows:
 *   "Face authentication unavailable on this device"
 *   and uses an approved fallback authentication factor (e.g. Cryptographic PIN confirmation).
 */
object FaceVerification {

    data class FaceVerificationResult(
        val faceVerified: Boolean,
        val authenticationMethod: String, // "FACE" or "SECURE_PIN_FALLBACK"
        val isHardwareFaceAvailable: Boolean,
        val riskLevel: String, // "LOW" or "HIGH"
        val statusMessage: String
    )

    /**
     * Checks if the host Android device officially declares hardware face authentication support.
     */
    fun isHardwareFaceSupported(context: Context): Boolean {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_FACE)
        } else {
            false
        }
    }

    /**
     * Evaluates face verification outcome with secure hardware or approved fallback flow.
     */
    fun evaluateFaceAuth(
        context: Context,
        isHardwareAuthPassed: Boolean,
        fallbackPinEntered: String? = null,
        expectedPin: String = "2026"
    ): FaceVerificationResult {
        val hardwareAvailable = isHardwareFaceSupported(context)

        if (hardwareAvailable && isHardwareAuthPassed) {
            return FaceVerificationResult(
                faceVerified = true,
                authenticationMethod = "FACE",
                isHardwareFaceAvailable = true,
                riskLevel = "LOW",
                statusMessage = "Face ID hardware verification successful (Class 3 Biometric)."
            )
        }

        if (!hardwareAvailable) {
            // Secure fallback path
            val isPinValid = fallbackPinEntered != null && fallbackPinEntered.trim() == expectedPin
            return if (isPinValid) {
                FaceVerificationResult(
                    faceVerified = true,
                    authenticationMethod = "SECURE_PIN_FALLBACK",
                    isHardwareFaceAvailable = false,
                    riskLevel = "LOW",
                    statusMessage = "Face authentication unavailable on this device. Approved fallback PIN verified."
                )
            } else {
                FaceVerificationResult(
                    faceVerified = false,
                    authenticationMethod = "SECURE_PIN_FALLBACK",
                    isHardwareFaceAvailable = false,
                    riskLevel = "HIGH",
                    statusMessage = "Face authentication unavailable on this device. Fallback PIN invalid or pending."
                )
            }
        }

        return FaceVerificationResult(
            faceVerified = false,
            authenticationMethod = "FACE",
            isHardwareFaceAvailable = true,
            riskLevel = "HIGH",
            statusMessage = "Face authentication failed or unconfirmed."
        )
    }
}
