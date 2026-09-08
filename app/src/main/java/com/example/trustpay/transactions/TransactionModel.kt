package com.example.trustpay.transactions

import com.example.trustpay.security.TransactionRiskEngine

data class PaymentTransactionRecord(
    val transactionId: String,
    val senderEmail: String,
    val recipient: String,
    val amount: Double,
    val currency: String = "INR",
    val status: String, // "APPROVED", "BLOCKED", "PENDING_VERIFICATION", "TAMPER_DETECTED"
    val riskLevel: String, // "LOW", "MEDIUM", "HIGH"
    val riskScore: Int,
    val deviceVerified: Boolean,
    val biometricVerified: Boolean,
    val faceVerified: Boolean,
    val otpVerified: Boolean,
    val bindingHash: String,
    val recommendedAction: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

enum class VerificationPhase {
    IDLE,
    STEP_1_DEVICE,
    STEP_2_BIOMETRIC,
    STEP_3_FACE,
    DEMO_BANK_OTP,
    AI_RISK_ANALYSIS,
    SECURITY_DASHBOARD_REVIEW,
    COMPLETED
}
