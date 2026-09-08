package com.example.trustpay.transactions

import com.example.trustpay.ai.GeminiSecurityAnalysis
import com.example.trustpay.security.DemoBankOtp
import com.example.trustpay.security.DeviceRecognition
import com.example.trustpay.security.FaceVerification
import com.example.trustpay.security.LocationSecurity
import com.example.trustpay.security.TransactionRiskEngine

/**
 * Transaction Verification Pipeline Orchestrator.
 *
 * Coordinates:
 *   STEP 1: Device Recognition
 *     ↓
 *   STEP 2: Biometric Authentication
 *     ↓
 *   STEP 3: Face Authentication
 *     ↓
 *   DEMO BANK OTP
 *     ↓
 *   AI Transaction Risk Analysis
 *     ↓
 *   Backend Security Rules
 */
object TransactionVerification {

    data class VerificationSession(
        val transactionId: String,
        val senderEmail: String,
        val recipient: String,
        val amount: Double,
        val bindingHash: String,
        var currentPhase: VerificationPhase = VerificationPhase.STEP_1_DEVICE,

        // Step outcomes
        var deviceResult: DeviceRecognition.DeviceRecognitionResult? = null,
        var biometricVerified: Boolean = false,
        var faceResult: FaceVerification.FaceVerificationResult? = null,
        var otpVerified: Boolean = false,
        var locationResult: LocationSecurity.LocationSecurityResult? = null,

        // AI assessment
        var aiAssessment: GeminiSecurityAnalysis.AiSecurityResponse? = null,

        // Final authorization
        var finalOutcome: TransactionAuthorization.AuthorizationOutcome? = null,

        // Attempts tracking
        var failedBiometricAttempts: Int = 0,
        var failedOtpAttempts: Int = 0,
        var userConfirmedExtraWarning: Boolean = false,
        var errorMessage: String? = null
    )

    fun startSession(
        transactionId: String,
        senderEmail: String,
        recipient: String,
        amount: Double,
        bindingHash: String
    ): VerificationSession {
        return VerificationSession(
            transactionId = transactionId,
            senderEmail = senderEmail,
            recipient = recipient,
            amount = amount,
            bindingHash = bindingHash,
            currentPhase = VerificationPhase.STEP_1_DEVICE
        )
    }

    /**
     * Executes AI Risk Analysis and prepares recommendation.
     */
    fun performAiAnalysis(session: VerificationSession): GeminiSecurityAnalysis.AiSecurityResponse {
        val analysis = GeminiSecurityAnalysis.analyzeTransaction(
            transactionId = session.transactionId,
            amount = session.amount,
            currency = "INR",
            recipient = session.recipient,
            deviceVerified = session.deviceResult?.deviceVerified == true,
            biometricVerified = session.biometricVerified,
            faceVerified = session.faceResult?.faceVerified == true,
            otpVerified = session.otpVerified,
            locationAnomaly = session.locationResult?.locationAnomaly == true,
            failedAttempts = session.failedBiometricAttempts + session.failedOtpAttempts
        )
        session.aiAssessment = analysis
        return analysis
    }

    /**
     * Completes authorization via deterministic backend rules.
     */
    fun completeAuthorization(session: VerificationSession): TransactionAuthorization.AuthorizationOutcome {
        val aiAssessment = session.aiAssessment ?: performAiAnalysis(session)
        val outcome = TransactionAuthorization.authorizeTransaction(
            transactionId = session.transactionId,
            senderEmail = session.senderEmail,
            recipient = session.recipient,
            amount = session.amount,
            bindingHash = session.bindingHash,
            deviceVerified = session.deviceResult?.deviceVerified == true,
            biometricVerified = session.biometricVerified,
            faceVerified = session.faceResult?.faceVerified == true,
            otpVerified = session.otpVerified,
            aiAssessment = aiAssessment,
            userConfirmedExtraVerification = session.userConfirmedExtraWarning
        )
        session.finalOutcome = outcome
        session.currentPhase = VerificationPhase.COMPLETED
        return outcome
    }
}
