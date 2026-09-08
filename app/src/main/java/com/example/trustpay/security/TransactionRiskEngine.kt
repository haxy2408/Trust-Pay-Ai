package com.example.trustpay.security

/**
 * Central Transaction Security Engine.
 *
 * Combines signals from:
 * - Device Recognition (STEP 1)
 * - Biometric Authentication (STEP 2)
 * - Face ID / Authentication (STEP 3)
 * - Demo Bank OTP
 * - Location Security
 * - Transaction amount & velocity history
 * - Recipient reputation
 *
 * Produces deterministic risk classification: LOW, MEDIUM, HIGH.
 */
object TransactionRiskEngine {

    enum class RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    data class SecurityCheckState(
        val device: String,    // "PASS", "FAIL", "WARNING"
        val biometric: String, // "PASS", "FAIL", "PENDING"
        val face: String,      // "PASS", "FAIL", "FALLBACK_PASS", "PENDING"
        val otp: String        // "PASS", "FAIL", "PENDING"
    )

    data class RiskEvaluationResult(
        val transactionId: String,
        val riskLevel: RiskLevel,
        val riskScore: Int, // 0 to 100
        val securityChecks: SecurityCheckState,
        val reasonCodes: List<String>,
        val recommendedAction: String, // "ALLOW", "EXTRA_VERIFICATION", "BLOCK"
        val details: String
    )

    // Known trusted merchant VPAs
    val TRUSTED_RECIPIENTS = setOf(
        "coffee.shop@okhdfcbank",
        "merchant.pay@okhdfcbank",
        "supermarket.groceries@icici",
        "utility.bills@axisbank",
        "metro.commute@sbi"
    )

    /**
     * Evaluates comprehensive transaction security risk.
     */
    fun evaluate(
        transactionId: String,
        amount: Double,
        recipient: String,
        deviceResult: DeviceRecognition.DeviceRecognitionResult,
        biometricPassed: Boolean,
        facePassed: Boolean,
        otpPassed: Boolean,
        failedAttemptsCount: Int = 0,
        isNewRecipient: Boolean = false,
        isUnusualAmount: Boolean = false,
        locationAnomaly: Boolean = false,
        velocityBreached: Boolean = false
    ): RiskEvaluationResult {
        var score = 10 // baseline nominal score
        val reasons = mutableListOf<String>()

        // 1. Device Evaluation
        val deviceStatus: String
        if (!deviceResult.deviceVerified || deviceResult.deviceStatus == "UNRECOGNIZED") {
            score += 35
            reasons.add("Unrecognized hardware signature / New device")
            deviceStatus = "FAIL"
        } else {
            deviceStatus = "PASS"
        }

        // 2. Recipient Evaluation
        val isKnown = TRUSTED_RECIPIENTS.contains(recipient.trim().lowercase())
        if (isNewRecipient || !isKnown) {
            score += 15
            reasons.add("New or unverified beneficiary address")
        }

        // 3. Amount & Velocity Evaluation
        if (amount >= 50000.0 || isUnusualAmount) {
            score += 20
            reasons.add("High-value transfer exceeding routine spending threshold")
        }
        if (velocityBreached) {
            score += 25
            reasons.add("Rolling transaction frequency anomaly (>3 txns or ₹50k in 5 min)")
        }

        // 4. Failed Attempts Evaluation
        if (failedAttemptsCount > 0) {
            val penalty = (failedAttemptsCount * 15).coerceAtMost(30)
            score += penalty
            reasons.add("Recent authentication failures ($failedAttemptsCount incident(s))")
        }

        // 5. Location Anomaly
        if (locationAnomaly) {
            score += 20
            reasons.add("Unusual geographic context / Out-of-perimeter request")
        }

        // 6. Security Check Status
        val biometricStatus = if (biometricPassed) "PASS" else "FAIL"
        val faceStatus = if (facePassed) "PASS" else "FAIL"
        val otpStatus = if (otpPassed) "PASS" else "FAIL"

        if (!biometricPassed) {
            score += 25
            reasons.add("Biometric intent verification incomplete or failed")
        }
        if (!facePassed) {
            score += 15
            reasons.add("Face ID verification pending or unconfirmed")
        }
        if (!otpPassed) {
            score += 20
            reasons.add("Demo bank OTP not validated")
        }

        val finalScore = score.coerceIn(0, 100)

        // Determine Risk Category and Recommended Action
        val riskLevel: RiskLevel
        val recommendedAction: String

        when {
            finalScore >= 70 || deviceStatus == "FAIL" && !biometricPassed -> {
                riskLevel = RiskLevel.HIGH
                recommendedAction = "BLOCK"
            }
            finalScore >= 35 || reasons.size >= 2 || !otpPassed -> {
                riskLevel = RiskLevel.MEDIUM
                recommendedAction = "EXTRA_VERIFICATION"
            }
            else -> {
                riskLevel = RiskLevel.LOW
                recommendedAction = "ALLOW"
            }
        }

        val checks = SecurityCheckState(
            device = deviceStatus,
            biometric = biometricStatus,
            face = faceStatus,
            otp = otpStatus
        )

        return RiskEvaluationResult(
            transactionId = transactionId,
            riskLevel = riskLevel,
            riskScore = finalScore,
            securityChecks = checks,
            reasonCodes = reasons,
            recommendedAction = recommendedAction,
            details = when (riskLevel) {
                RiskLevel.LOW -> "Transaction posture verified. Cryptographic checks satisfied."
                RiskLevel.MEDIUM -> "Elevated risk signals detected. Additional verification mandated."
                RiskLevel.HIGH -> "High-severity risk indicators present. Payment blocked by security policy."
            }
        )
    }
}
