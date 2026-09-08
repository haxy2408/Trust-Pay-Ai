package com.example.trustpay.backend

import com.example.trustpay.authentication.LoginManager
import com.example.trustpay.transactions.PaymentTransactionRecord

/**
 * Backend Transaction API.
 * Server-side payment processing, deterministic balance updates,
 * and immutable transaction ledger persistence.
 */
object TransactionApi {

    private val transactionLedger = mutableListOf<PaymentTransactionRecord>()

    init {
        // Seed initial demo history
        transactionLedger.add(
            PaymentTransactionRecord(
                transactionId = "TXN-98401",
                senderEmail = "alex.pay@trustpay.demo",
                recipient = "merchant.pay@okhdfcbank",
                amount = 450.0,
                status = "APPROVED",
                riskLevel = "LOW",
                riskScore = 12,
                deviceVerified = true,
                biometricVerified = true,
                faceVerified = true,
                otpVerified = true,
                bindingHash = "7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069",
                recommendedAction = "ALLOW",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 15,
                notes = "Routine merchant checkout. Verified."
            )
        )
    }

    fun getTransactions(userEmail: String?): List<PaymentTransactionRecord> {
        return if (userEmail == null) {
            transactionLedger.toList()
        } else {
            transactionLedger.filter { it.senderEmail == userEmail }
        }
    }

    fun getAllTransactions(): List<PaymentTransactionRecord> = transactionLedger.toList()

    fun recordTransaction(record: PaymentTransactionRecord): Boolean {
        transactionLedger.add(0, record)
        SecurityApi.logSecurityEvent(
            eventType = if (record.status == "APPROVED") "TRANSACTION_SETTLED" else "TRANSACTION_RECORDED",
            description = "${record.transactionId} [${record.status}] ₹${"%,.2f".format(record.amount)} to ${record.recipient}. Risk: ${record.riskLevel} (${record.riskScore}/100)",
            severity = if (record.status == "APPROVED") "SUCCESS" else if (record.status == "BLOCKED") "CRITICAL" else "WARNING",
            metadataHash = record.bindingHash
        )
        return true
    }

    fun resetBalance(email: String, newBalance: Double) {
        LoginManager.updateBalance(email, newBalance)
        SecurityApi.logSecurityEvent(
            eventType = "BALANCE_RESET",
            description = "Demo account balance reset to ₹${"%,.2f".format(newBalance)} for $email",
            severity = "INFO"
        )
    }
}
