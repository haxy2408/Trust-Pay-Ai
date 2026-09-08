package com.example.trustpay.transactions

import com.example.trustpay.authentication.LoginManager
import java.security.MessageDigest
import java.util.UUID

/**
 * Transaction Creation Module.
 *
 * Validates inputs, generates cryptographic binding digests,
 * and initiates verification pipelines.
 */
object TransactionCreation {

    data class CreationRequest(
        val senderEmail: String,
        val recipientVpa: String,
        val amount: Double,
        val currency: String = "INR",
        val clientNote: String = ""
    )

    data class CreationResult(
        val isValid: Boolean,
        val transactionId: String,
        val bindingHash: String,
        val errorMessage: String? = null
    )

    fun createTransaction(request: CreationRequest): CreationResult {
        // Validate recipient format
        val recipient = request.recipientVpa.trim()
        if (recipient.isBlank() || (!recipient.contains("@") && !recipient.matches(Regex("^[0-9]{10}$")))) {
            return CreationResult(
                isValid = false,
                transactionId = "",
                bindingHash = "",
                errorMessage = "Invalid recipient format. Please enter a valid UPI ID (e.g., merchant@bank) or 10-digit number."
            )
        }

        // Validate amount
        if (request.amount <= 0.0) {
            return CreationResult(
                isValid = false,
                transactionId = "",
                bindingHash = "",
                errorMessage = "Payment amount must be greater than ₹0.00."
            )
        }

        // Check account balance
        val user = LoginManager.getUser(request.senderEmail)
        if (user != null && user.accountBalance < request.amount) {
            return CreationResult(
                isValid = false,
                transactionId = "",
                bindingHash = "",
                errorMessage = "Insufficient funds. Available balance: ₹${"%,.2f".format(user.accountBalance)}."
            )
        }

        val txnId = "TXN-" + (10000 + (System.currentTimeMillis() % 90000))
        val bindingHash = computeBindingDigest(txnId, recipient, request.amount, request.currency)

        return CreationResult(
            isValid = true,
            transactionId = txnId,
            bindingHash = bindingHash,
            errorMessage = null
        )
    }

    fun computeBindingDigest(transactionId: String, recipient: String, amount: Double, currency: String): String {
        val payload = "TXN_BIND::$transactionId::$recipient::${"%.2f".format(amount)}::$currency"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
