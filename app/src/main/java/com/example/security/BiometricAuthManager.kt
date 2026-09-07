package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Encapsulates Android BiometricPrompt authentication.
 *
 * Security Requirements:
 * - Uses Android BiometricPrompt for secure device authentication.
 * - Supports any enrolled fingerprint, face, or secure device credential (PIN/pattern/password).
 * - Zero access or storage of biometric data (delegated completely to Android OS TEE/SE).
 * - If cancelled, failed, or rejected, invokes appropriate failure callback.
 */
class BiometricAuthManager(private val context: Context) {

    /**
     * Checks whether biometric or device credential authentication is available on the device.
     */
    fun checkBiometricAvailability(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.TEMPORARILY_UNAVAILABLE
            else -> BiometricStatus.UNSUPPORTED
        }
    }

    /**
     * Launches the system BiometricPrompt modal.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }

    enum class BiometricStatus {
        AVAILABLE,
        NOT_ENROLLED,
        NO_HARDWARE,
        TEMPORARILY_UNAVAILABLE,
        UNSUPPORTED
    }
}
