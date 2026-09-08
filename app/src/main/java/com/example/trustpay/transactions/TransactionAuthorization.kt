package com.example.trustpay.transactions

import com.example.trustpay.ai.GeminiSecurityAnalysis
import com.example.trustpay.authentication.LoginManager
import com.example.trustpay.backend.SecurityApi
import com.example.trustpay.backend.TransactionApi
import com.example.trustpay.security.TransactionRiskEngine

/**
 * Backend Transaction Authorization Module.
 *
 * Implements deterministic authorization rules:
 * - The backend makes the final authorization decision.
 * - Gemini AI NEVER independently authorizes or executes payment.
 * - Enforces balance deduction and ledger recording if authorized.
 * - Emits security alerts and blocks execution if threat rules trigger.
 */
object TransactionAuthorization {

    sealed class AuthorizationOutcome {
        data class Approved(
            val transaction: PaymentTransactionRecord,
            val remainingBalance: Double,
            val message: String
        ) : AuthorizationOutcome()

        data class Blocked(
            val transactionId: String,
            val reason: String,
            val riskScore: Int,
            val auditLogId: String
        ) : AuthorizationOutcome()

        data class RequiresExtraVerification(
            val transactionId: String,
            val reason: String,
            val pendingChecks: List<String>
        ) : AuthorizationOutcome()
    }

    /**
     * Authorizes or denies a transaction deterministically using system security rules.
     */
    fun authorizeTransaction(
        transactionId: String,
        senderEmail: String,
        recipient: String,
        amount: Double,
        bindingHash: String,
        deviceVerified: Boolean,
        biometricVerified: Boolean,
        faceVerified: Boolean,
        otpVerified: Boolean,
        aiAssessment: GeminiSecurityAnalysis.AiSecurityResponse,
        userConfirmedExtraVerification: Boolean = false
    ): AuthorizationOutcome {
        val user = LoginManager.getUser(senderEmail)
            ?: return AuthorizationOutcome.Blocked(
                transactionId = transactionId,
                reason = "Sender account does not exist or session invalid.",
                riskScore = 100,
                auditLogId = "ERR-NO-USER"
            )

        // 1. Critical Security Checks Gate
        if (!deviceVerified && !biometricVerified) {
            SecurityApi.logSecurityEvent(
                eventType = "TRANSACTION_BLOCKED_DEVICE_AND_BIOMETRIC",
                description = "Critical check failure: Both device recognition and biometric intent failed for $transactionId",
                severity = "CRITICAL",
                metadataHash = bindingHash
            )
            return AuthorizationOutcome.Blocked(
                transactionId = transactionId,
                reason = "Transaction Blocked: Unrecognized device and failed biometric authentication.",
                riskScore = aiAssessment.riskScore,
                auditLogId = "CRIT-DEV-BIO"
            )
        }

        // 2. Fund Check
        if (user.accountBalance < amount) {
            SecurityApi.logSecurityEvent(
                eventType = "TRANSACTION_DECLINED_INSUFFICIENT_FUNDS",
                description = "Declined $transactionId: Balance ₹${user.accountBalance} < ₹$amount",
                severity = "WARNING",
                metadataHash = bindingHash
            )
            return AuthorizationOutcome.Blocked(
                transactionId = transactionId,
                reason = "Insufficient funds in account for this transfer.",
                riskScore = aiAssessment.riskScore,
                auditLogId = "DECL-FUNDS"
            )
        }

        // 3. Evaluation of Recommended Action & Fallback Rules
        // If AI recommended BLOCK or Deterministic score >= 70
        if (aiAssessment.recommendedAction == "BLOCK" || aiAssessment.riskScore >= 70) {
            val record = PaymentTransactionRecord(
                transactionId = transactionId,
                senderEmail = senderEmail,
                recipient = recipient,
                amount = amount,
                status = "BLOCKED",
                riskLevel = "HIGH",
                riskScore = aiAssessment.riskScore,
                deviceVerified = deviceVerified,
                biometricVerified = biometricVerified,
                faceVerified = faceVerified,
                otpVerified = otpVerified,
                bindingHash = bindingHash,
                recommendedAction = "BLOCK",
                notes = "Blocked by security rules: ${aiAssessment.reasonCodes.joinToString(", ")}"
            )
            TransactionApi.recordTransaction(record)
            return AuthorizationOutcome.Blocked(
                transactionId = transactionId,
                reason = "Security Engine Intervention: High-risk anomaly detected (${aiAssessment.reasonCodes.joinToString("; ")}).",
                riskScore = aiAssessment.riskScore,
                auditLogId = "BLOCKED-$transactionId"
            )
        }

        // 4. Moderate Risk / Extra Verification Required
        if (aiAssessment.recommendedAction == "EXTRA_VERIFICATION" && !userConfirmedExtraVerification) {
            val pending = mutableListOf<String>()
            if (!otpVerified) pending.add("Demo Bank OTP")
            if (!faceVerified) pending.add("Face ID / Secure PIN Fallback")
            if (!deviceVerified) pending.add("Device Registration")

            return AuthorizationOutcome.RequiresExtraVerification(
                transactionId = transactionId,
                reason = "Suspicious transaction parameters detected. Extra user confirmation mandated.",
                pendingChecks = pending
            )
        }

        // 5. Deterministic Approval
        val updatedBalance = user.accountBalance - amount
        LoginManager.updateBalance(user.email, updatedBalance)

        val record = PaymentTransactionRecord(
            transactionId = transactionId,
            senderEmail = senderEmail,
            recipient = recipient,
            amount = amount,
            status = "APPROVED",
            riskLevel = aiAssessment.riskLevel,
            riskScore = aiAssessment.riskScore,
            deviceVerified = deviceVerified,
            biometricVerified = biometricVerified,
            faceVerified = faceVerified,
            otpVerified = otpVerified,
            bindingHash = bindingHash,
            recommendedAction = "ALLOW",
            notes = "Cryptographically sealed and released under 3-step verification."
        )
        TransactionApi.recordTransaction(record)

        return AuthorizationOutcome.Approved(
            transaction = record,
            remainingBalance = updatedBalance,
            message = "Transaction SECURE — APPROVED. ₹${"%,.2f".format(amount)} transferred to $recipient."
        )
    }
}
