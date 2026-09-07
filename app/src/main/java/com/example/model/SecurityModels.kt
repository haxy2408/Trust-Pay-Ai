package com.example.model

import java.util.Locale

/**
 * Model representing a completed or recorded transaction in TrustPay AI.
 */
data class PaymentTransaction(
    val id: String,
    val recipient: String,
    val amount: Double,
    val currency: String = "INR",
    val timestamp: Long,
    val payloadHash: String,
    val status: String,
    val riskScore: Int,
    val signals: List<String>,
    val balanceAfter: Double? = null
)

/**
 * Formats a currency amount into Indian Rupee numbering notation (e.g., ₹1,00,000.00).
 */
fun formatIndianCurrency(amount: Double): String {
    val isNegative = amount < 0
    val absAmount = Math.abs(amount)
    val formatted = String.format(Locale.US, "%.2f", absAmount)
    val parts = formatted.split(".")
    val intPart = parts[0]
    val decPart = parts[1]

    val formattedInt = if (intPart.length <= 3) {
        intPart
    } else {
        val last3 = intPart.substring(intPart.length - 3)
        val remaining = intPart.substring(0, intPart.length - 3)
        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count % 2 == 0 && i != 0) {
                sb.append(",")
            }
        }
        sb.reverse().toString() + "," + last3
    }

    return (if (isNegative) "-" else "") + "₹" + formattedInt + "." + decPart
}

/**
 * Audit log entry for tracking security and lifecycle events.
 */
data class AuditLogEntry(
    val id: String,
    val timestamp: Long,
    val title: String,
    val details: String,
    val status: AuditStatus,
    val hash: String? = null
)

enum class AuditStatus {
    SUCCESS,
    WARNING,
    DANGER,
    INFO
}

/**
 * Stages for the multi-step verification process.
 */
enum class VerificationStep {
    IDLE,
    BIOMETRIC,
    CHALLENGE,
    DEMO_OTP,
    COMPLETED,
    BLOCKED
}

/**
 * Types of random challenges required during Velocity Anomaly verification.
 */
enum class ChallengeType {
    AMOUNT,
    RECIPIENT_LAST_4
}

/**
 * Challenge details presented to the user.
 */
data class ChallengeData(
    val type: ChallengeType,
    val question: String,
    val expectedAnswer: String
)

/**
 * Result of initial risk & signal analysis before authorization.
 */
data class PaymentAnalysisResult(
    val transactionId: String,
    val recipient: String,
    val amount: Double,
    val currency: String = "INR",
    val boundHash: String,
    val riskScore: Int,
    val signals: List<String>,
    val requiresBiometricOnly: Boolean,
    val requires3StepVerification: Boolean,
    val canApproveDirectly: Boolean,
    val designatedFinger: String? = null
)

/**
 * Snapshot of critical transaction details locked in during the authentication phase.
 * Used for post-auth re-verification before final payment confirmation.
 */
data class AuthenticatedTransactionSnapshot(
    val transactionId: String,
    val recipient: String,
    val amount: Double,
    val currency: String = "INR",
    val boundHash: String,
    val authTimestamp: Long = System.currentTimeMillis(),
    val biometricVerified: Boolean = false,
    val designatedFinger: String? = null
)

/**
 * Result of the re-verification check between authentication and final confirmation.
 */
data class IntegrityCheckResult(
    val isValid: Boolean,
    val discrepancyDescription: String? = null
)

/**
 * Active verification workflow state.
 */
data class VerificationState(
    val transactionId: String,
    val recipient: String,
    val amount: Double,
    val currency: String = "INR",
    val boundHash: String,
    val currentStep: VerificationStep,
    val isVelocityAnomaly: Boolean,
    val challengeData: ChallengeData?,
    val challengeInput: String = "",
    val challengeError: String? = null,
    val demoOtp: String?,
    val otpInput: String = "",
    val otpError: String? = null,
    val isBiometricCompleted: Boolean = false,
    val designatedFinger: String? = null,
    val authenticatedSnapshot: AuthenticatedTransactionSnapshot? = null
)

enum class FinalDecisionStatus {
    NONE,
    APPROVED,
    BLOCKED,
    TAMPER_DETECTED
}
