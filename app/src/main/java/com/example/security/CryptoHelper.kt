package com.example.security

import com.example.model.AuthenticatedTransactionSnapshot
import com.example.model.IntegrityCheckResult
import java.security.MessageDigest
import java.util.Locale

/**
 * Cryptographic helper providing SHA-256 payload binding and integrity verification.
 *
 * Security Rule 3 & Enhanced Anti-Tamper:
 * Binds recipient, amount, currency, and unique transaction ID with SHA-256.
 * Re-verifies all critical parameters against the authentication snapshot before final payment confirmation.
 * If any parameter changes after analysis or authentication, tampering is flagged
 * and payment is halted immediately with "Transaction tampering detected".
 */
object CryptoHelper {

    /**
     * Computes the SHA-256 hash digest of transaction parameters.
     * Binding format: "$transactionId|$recipient|$amount|$currency"
     */
    fun computeBindingHash(
        transactionId: String,
        recipient: String,
        amount: Double,
        currency: String = "INR"
    ): String {
        val normalizedAmount = String.format(Locale.US, "%.2f", amount)
        val rawPayload = "$transactionId|$recipient|$normalizedAmount|$currency"
        return sha256(rawPayload)
    }

    /**
     * Verifies whether current transaction parameters match the cryptographically bound hash.
     */
    fun verifyBinding(
        boundHash: String,
        transactionId: String,
        recipient: String,
        amount: Double,
        currency: String = "INR"
    ): Boolean {
        val currentHash = computeBindingHash(transactionId, recipient, amount, currency)
        return boundHash.equals(currentHash, ignoreCase = true)
    }

    /**
     * Re-verifies all critical transaction details against the details locked in
     * during the authentication phase:
     * - Recipient UPI ID
     * - Payment Amount
     * - Currency
     * - Transaction ID
     * - SHA-256 Payload Signature
     */
    fun verifyTransactionIntegrity(
        snapshot: AuthenticatedTransactionSnapshot,
        candidateTransactionId: String,
        candidateRecipient: String,
        candidateAmount: Double,
        candidateCurrency: String = "INR"
    ): IntegrityCheckResult {
        // 1. Transaction ID integrity
        if (snapshot.transactionId != candidateTransactionId) {
            return IntegrityCheckResult(
                isValid = false,
                discrepancyDescription = "Transaction ID mismatch: Authenticated '${snapshot.transactionId}', wire found '$candidateTransactionId'"
            )
        }

        // 2. Recipient UPI ID integrity
        if (!snapshot.recipient.equals(candidateRecipient.trim(), ignoreCase = true)) {
            return IntegrityCheckResult(
                isValid = false,
                discrepancyDescription = "Recipient UPI ID mismatch: Authenticated '${snapshot.recipient}', but destination altered to '$candidateRecipient'"
            )
        }

        // 3. Payment Amount integrity
        if (Math.abs(snapshot.amount - candidateAmount) > 0.001) {
            return IntegrityCheckResult(
                isValid = false,
                discrepancyDescription = "Amount mismatch: Authenticated ₹${String.format(Locale.US, "%.2f", snapshot.amount)}, but wire mutated to ₹${String.format(Locale.US, "%.2f", candidateAmount)}"
            )
        }

        // 4. Currency integrity
        if (!snapshot.currency.equals(candidateCurrency.trim(), ignoreCase = true)) {
            return IntegrityCheckResult(
                isValid = false,
                discrepancyDescription = "Currency mismatch: Authenticated '${snapshot.currency}', but wire mutated to '$candidateCurrency'"
            )
        }

        // 5. Cryptographic SHA-256 hash integrity
        val isBindingValid = verifyBinding(
            boundHash = snapshot.boundHash,
            transactionId = candidateTransactionId,
            recipient = candidateRecipient,
            amount = candidateAmount,
            currency = candidateCurrency
        )

        if (!isBindingValid) {
            val candidateHash = computeBindingHash(candidateTransactionId, candidateRecipient, candidateAmount, candidateCurrency)
            return IntegrityCheckResult(
                isValid = false,
                discrepancyDescription = "Cryptographic signature mismatch: Bound SHA-256 '${snapshot.boundHash.take(16)}...' differs from recomputed hash '${candidateHash.take(16)}...'"
            )
        }

        return IntegrityCheckResult(isValid = true, discrepancyDescription = null)
    }

    /**
     * Low-level SHA-256 hashing.
     */
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
