package com.example.trustpay.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * STEP 2 — Biometric Authentication Security Module.
 *
 * Requirements:
 * - Uses operating system's secure biometric authentication mechanism.
 * - Supports fingerprint/biometric where available.
 * - NEVER stores raw fingerprints or biometric images.
 * - Receives ONLY authentication result (Success / Failure).
 * - Tracks failure count, denies transaction or limits retries (max 3).
 * - Temporarily locks transaction after repeated failures.
 * - Do not implement fake photo comparison; use platform's secure biometric APIs.
 */
object BiometricVerification {

    enum class BiometricState {
        AVAILABLE,
        NOT_AVAILABLE,
        AUTHENTICATED,
        FAILED,
        CANCELLED,
        LOCKED_OUT
    }

    data class BiometricResult(
        val biometricVerified: Boolean,
        val state: BiometricState,
        val authenticationMethod: String = "BIOMETRIC",
        val riskLevel: String, // "LOW" or "HIGH"
        val attemptsUsed: Int,
        val isLocked: Boolean = false,
        val statusMessage: String
    )

    private const val MAX_BIOMETRIC_ATTEMPTS = 3
    private var failedAttemptsCount = 0
    private var isTemporarilyLocked = false

    fun canAuthenticate(context: Context): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
    }

    fun isBiometricHardwareAvailable(context: Context): Boolean {
        val status = canAuthenticate(context)
        return status == BiometricManager.BIOMETRIC_SUCCESS || status == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    }

    fun getDeviceBiometricState(context: Context): BiometricState {
        return if (isTemporarilyLocked) {
            BiometricState.LOCKED_OUT
        } else if (isBiometricHardwareAvailable(context)) {
            BiometricState.AVAILABLE
        } else {
            BiometricState.NOT_AVAILABLE
        }
    }

    fun getFailedAttempts(): Int = failedAttemptsCount
    fun isLocked(): Boolean = isTemporarilyLocked

    fun resetLock() {
        failedAttemptsCount = 0
        isTemporarilyLocked = false
    }

    fun recordFailure(): BiometricResult {
        failedAttemptsCount++
        if (failedAttemptsCount >= MAX_BIOMETRIC_ATTEMPTS) {
            isTemporarilyLocked = true
            return BiometricResult(
                biometricVerified = false,
                state = BiometricState.LOCKED_OUT,
                authenticationMethod = "BIOMETRIC",
                riskLevel = "HIGH",
                attemptsUsed = failedAttemptsCount,
                isLocked = true,
                statusMessage = "Maximum biometric attempts exceeded ($failedAttemptsCount/$MAX_BIOMETRIC_ATTEMPTS). Transaction temporarily locked."
            )
        }
        return BiometricResult(
            biometricVerified = false,
            state = BiometricState.FAILED,
            authenticationMethod = "BIOMETRIC",
            riskLevel = "HIGH",
            attemptsUsed = failedAttemptsCount,
            isLocked = false,
            statusMessage = "Biometric authentication failed. Attempt $failedAttemptsCount of $MAX_BIOMETRIC_ATTEMPTS."
        )
    }

    fun recordSuccess(): BiometricResult {
        val used = failedAttemptsCount
        resetLock()
        return BiometricResult(
            biometricVerified = true,
            state = BiometricState.AUTHENTICATED,
            authenticationMethod = "BIOMETRIC",
            riskLevel = "LOW",
            attemptsUsed = used,
            isLocked = false,
            statusMessage = "Biometric intent verified successfully."
        )
    }

    /**
     * Launches the system BiometricPrompt if activity is available.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "TrustPay Biometric Authorization",
        subtitle: String = "Confirm transfer intent using your fingerprint or device credentials",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onStateChange: ((BiometricState, String) -> Unit)? = null
    ) {
        if (isTemporarilyLocked) {
            onStateChange?.invoke(BiometricState.LOCKED_OUT, "Biometric authentication is temporarily locked due to repeated failures.")
            onError("Biometric authentication is temporarily locked due to repeated failures.")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    recordSuccess()
                    onStateChange?.invoke(BiometricState.AUTHENTICATED, "Biometric intent verified successfully.")
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    val state = when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON -> BiometricState.CANCELLED
                        BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            isTemporarilyLocked = true
                            BiometricState.LOCKED_OUT
                        }
                        BiometricPrompt.ERROR_HW_NOT_PRESENT, BiometricPrompt.ERROR_HW_UNAVAILABLE, BiometricPrompt.ERROR_NO_BIOMETRICS -> BiometricState.NOT_AVAILABLE
                        else -> BiometricState.FAILED
                    }
                    onStateChange?.invoke(state, errString.toString())
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    val res = recordFailure()
                    onStateChange?.invoke(res.state, res.statusMessage)
                    onError("Fingerprint not recognized. Please retry.")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}
