package com.example.trustpay.backend

import com.example.trustpay.model.AuditStatus
import com.example.trustpay.model.PaymentTransaction
import com.example.trustpay.model.SecondSignatureResult
import com.example.trustpay.model.SecondSignatureStatus
import com.example.trustpay.model.SecondSignatureTransaction
import com.example.trustpay.security.CryptoUtils
import com.example.trustpay.security.SecondSignatureEngine
import com.example.trustpay.storage.TrustPayStorage
import java.util.UUID

/**
 * Backend Second Signature API.
 * Provides server-side transaction management, cryptographic fingerprinting,
 * independent authorization endpoints, replay protection, and audit logging.
 */
object SecondSignatureApi {

    /**
     * POST /transactions
     * Initiates a transaction requiring a second signature.
     * The server computes the canonical transaction fingerprint and creates the first signature.
     */
    fun createTransaction(
        senderEmail: String,
        senderName: String,
        recipientId: String,
        amount: Double,
        currency: String = "INR",
        transactionType: String = "HIGH_VALUE_PAYMENT",
        purposeDescription: String = "Enterprise Settlement",
        designatedSecondSignerId: String,
        designatedSecondSignerName: String,
        designatedSecondSignerRole: String = "Corporate Treasury Officer"
    ): SecondSignatureResult {
        // 1. Balance verification
        val balance = TrustPayStorage.getBalance(senderEmail)
        if (balance < amount) {
            return SecondSignatureResult(
                success = false,
                message = "Insufficient funds in sender account.",
                errorCode = "INSUFFICIENT_FUNDS"
            )
        }

        // 2. Generation of server parameters
        val txId = "TXN_2ND_${UUID.randomUUID().toString().take(8).uppercase()}"
        val nonce = UUID.randomUUID().toString().take(12)
        val createdAt = System.currentTimeMillis()
        val version = 1

        // 3. Server computes canonical transaction fingerprint
        val fingerprint = SecondSignatureEngine.computeTransactionFingerprint(
            transactionId = txId,
            senderId = senderEmail,
            recipientId = recipientId,
            amount = amount,
            currency = currency,
            createdAt = createdAt,
            transactionType = transactionType,
            transactionVersion = version,
            nonce = nonce
        )

        // 4. Server computes First Signature for primary signer
        val firstSig = SecondSignatureEngine.generateSignerSignature(
            signerId = senderEmail,
            signerName = senderName,
            signerRole = "Initiator / Primary Signer",
            transactionId = txId,
            transactionFingerprint = fingerprint,
            transactionVersion = version,
            timestamp = createdAt,
            nonce = nonce
        )

        // 5. Transaction enters SECOND_SIGNATURE_REQUIRED state
        val transaction = SecondSignatureTransaction(
            id = txId,
            senderId = senderEmail,
            senderName = senderName,
            recipientId = recipientId,
            amount = amount,
            currency = currency,
            transactionType = transactionType,
            purposeDescription = purposeDescription,
            status = SecondSignatureStatus.SECOND_SIGNATURE_REQUIRED,
            transactionVersion = version,
            transactionFingerprint = fingerprint,
            createdAt = createdAt,
            expiresAt = createdAt + SecondSignatureEngine.SECOND_SIGNATURE_EXPIRY_MS,
            designatedSecondSignerId = designatedSecondSignerId,
            designatedSecondSignerName = designatedSecondSignerName,
            designatedSecondSignerRole = designatedSecondSignerRole,
            firstSignature = firstSig,
            riskScore = if (amount >= 50000.0) 65 else 30,
            nonce = nonce
        )

        TrustPayStorage.addSecondSignatureTransaction(transaction)

        // 6. Immutable Security Audit Trail
        SecurityApi.logSecurityEvent(
            eventType = "SECOND_SIGNATURE_REQUIRED",
            description = "Transaction $txId (₹${"%,.2f".format(amount)} to $recipientId) initiated by $senderEmail. Designated 2nd signer: $designatedSecondSignerName.",
            severity = "WARNING",
            metadataHash = fingerprint
        )

        TrustPayStorage.addAuditLog(
            title = "2nd Signature Required",
            details = "High-risk transfer $txId locked. Waiting for secondary authorization from $designatedSecondSignerName.",
            status = AuditStatus.WARNING,
            hash = fingerprint
        )

        return SecondSignatureResult(
            success = true,
            message = "Transaction initiated and sealed with First Signature. Pending Second Signature.",
            transaction = transaction
        )
    }

    /**
     * POST /transactions/{id}/signature
     * Evaluates and records the second signature independently.
     */
    fun submitSecondSignature(
        transactionId: String,
        signerEmail: String,
        signerName: String,
        signerRole: String = "Authorized Co-Signer",
        simulatedTamperedAmount: Double? = null,
        simulatedTamperedRecipient: String? = null
    ): SecondSignatureResult {
        val tx = TrustPayStorage.getSecondSignatureTransaction(transactionId)
            ?: return SecondSignatureResult(
                success = false,
                message = "Transaction not found.",
                errorCode = "TRANSACTION_NOT_FOUND"
            )

        // Re-check balance before final authorization
        val senderBal = TrustPayStorage.getBalance(tx.senderId)
        if (senderBal < tx.amount) {
            val failedTx = tx.copy(
                status = SecondSignatureStatus.FAILED,
                failureReason = "Sender balance insufficient at time of second approval."
            )
            TrustPayStorage.updateSecondSignatureTransaction(failedTx)
            SecurityApi.logSecurityEvent(
                eventType = "TRANSACTION_EXECUTION_DECLINED",
                description = "Declined $transactionId: Sender ${tx.senderId} has insufficient funds.",
                severity = "CRITICAL",
                metadataHash = tx.transactionFingerprint
            )
            return SecondSignatureResult(
                success = false,
                message = "Transfer declined: Sender has insufficient balance.",
                transaction = failedTx,
                errorCode = "INSUFFICIENT_FUNDS"
            )
        }

        val result = SecondSignatureEngine.verifyAndAuthorizeSecondSignature(
            transaction = tx,
            secondSignerId = signerEmail,
            secondSignerName = signerName,
            secondSignerRole = signerRole,
            simulatedTamperedAmount = simulatedTamperedAmount,
            simulatedTamperedRecipient = simulatedTamperedRecipient
        )

        val updatedTx = result.transaction ?: tx
        TrustPayStorage.updateSecondSignatureTransaction(updatedTx)

        if (result.success && updatedTx.status == SecondSignatureStatus.EXECUTED) {
            // Atomic fund transfer
            TrustPayStorage.setBalance(senderBal - updatedTx.amount, updatedTx.senderId)

            // Record into primary ledger
            val primaryTx = PaymentTransaction(
                id = updatedTx.id,
                recipient = updatedTx.recipientId,
                amount = updatedTx.amount,
                currency = updatedTx.currency,
                status = "APPROVED",
                bindingHash = updatedTx.secondSignature?.signatureHash ?: updatedTx.transactionFingerprint,
                riskScore = updatedTx.riskScore,
                senderId = updatedTx.senderId,
                note = "[Dual-Signed: ${updatedTx.senderName} & $signerName] ${updatedTx.purposeDescription}",
                balanceAfter = TrustPayStorage.getBalance(updatedTx.senderId)
            )
            TrustPayStorage.addTransaction(primaryTx)

            SecurityApi.logSecurityEvent(
                eventType = "TRANSACTION_EXECUTED_TWO_SIGNATURES",
                description = "Transaction ${updatedTx.id} (₹${"%,.2f".format(updatedTx.amount)}) authorized by both $signerEmail & ${updatedTx.senderId}.",
                severity = "SUCCESS",
                metadataHash = updatedTx.secondSignature?.signatureHash
            )

            TrustPayStorage.addAuditLog(
                title = "2nd Signature Authorized & Executed",
                details = "Transaction ${updatedTx.id} released after independent cryptographic verification from $signerName.",
                status = AuditStatus.SUCCESS,
                hash = updatedTx.secondSignature?.signatureHash
            )
        } else {
            // Log security failure
            SecurityApi.logSecurityEvent(
                eventType = "SECOND_SIGNATURE_REJECTED_OR_FAILED",
                description = "Approval failed for ${updatedTx.id}: ${result.message} [Code: ${result.errorCode}]",
                severity = if (result.errorCode == "TRANSACTION_TAMPERED") "CRITICAL" else "WARNING",
                metadataHash = updatedTx.transactionFingerprint
            )

            TrustPayStorage.addAuditLog(
                title = "2nd Signature Verification Failed",
                details = "Attempt on ${updatedTx.id} failed: ${result.message}",
                status = if (result.errorCode == "TRANSACTION_TAMPERED") AuditStatus.DANGER else AuditStatus.WARNING,
                hash = updatedTx.transactionFingerprint
            )
        }

        return result
    }

    /**
     * POST /transactions/{id}/reject
     * Explicit rejection by the second signer.
     */
    fun rejectTransaction(transactionId: String, signerEmail: String, reason: String): Boolean {
        val tx = TrustPayStorage.getSecondSignatureTransaction(transactionId) ?: return false
        val rejectedTx = tx.copy(
            status = SecondSignatureStatus.REJECTED,
            rejectionReason = reason
        )
        TrustPayStorage.updateSecondSignatureTransaction(rejectedTx)

        SecurityApi.logSecurityEvent(
            eventType = "SECOND_SIGNATURE_REJECTED",
            description = "Transaction $transactionId rejected by $signerEmail: $reason",
            severity = "WARNING",
            metadataHash = tx.transactionFingerprint
        )

        TrustPayStorage.addAuditLog(
            title = "2nd Signature Rejected",
            details = "Transaction $transactionId was explicitly declined by $signerEmail. Reason: $reason",
            status = AuditStatus.DANGER,
            hash = tx.transactionFingerprint
        )
        return true
    }
}
