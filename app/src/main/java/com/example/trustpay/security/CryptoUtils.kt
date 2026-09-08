package com.example.trustpay.security

import com.example.trustpay.model.AuthenticatedTransactionSnapshot
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.NumberFormat
import java.util.Locale
import java.util.Random
import java.util.UUID
import kotlin.math.abs

object CryptoUtils {

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Tamper-evident SHA-256 binding digest:
     * Format: TXN_ID|RECIPIENT|AMOUNT|CURRENCY
     */
    fun computeBindingHash(
        transactionId: String,
        recipient: String,
        amount: Double,
        currency: String = "INR"
    ): String {
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val payload = "$transactionId|${recipient.trim()}|$formattedAmount|$currency"
        return sha256(payload)
    }

    fun createSnapshot(
        transactionId: String,
        recipient: String,
        amount: Double,
        currency: String = "INR"
    ): AuthenticatedTransactionSnapshot {
        val hash = computeBindingHash(transactionId, recipient, amount, currency)
        return AuthenticatedTransactionSnapshot(
            transactionId = transactionId,
            recipient = recipient.trim(),
            amount = amount,
            currency = currency,
            bindingHash = hash,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Critical Rule: Re-verifies transaction integrity before funds are released.
     */
    fun verifyTransactionIntegrity(
        snapshot: AuthenticatedTransactionSnapshot,
        currentRecipient: String,
        currentAmount: Double,
        currentCurrency: String = "INR",
        currentTxId: String = snapshot.transactionId
    ): Boolean {
        if (snapshot.transactionId != currentTxId) return false
        if (snapshot.recipient != currentRecipient.trim()) return false
        if (abs(snapshot.amount - currentAmount) > 0.001) return false
        if (snapshot.currency != currentCurrency) return false

        val recomputed = computeBindingHash(currentTxId, currentRecipient, currentAmount, currentCurrency)
        return recomputed == snapshot.bindingHash
    }

    fun generateTransactionId(): String {
        val chars = "0123456789ABCDEF"
        val rnd = Random()
        val sb = java.lang.StringBuilder("TXN_")
        for (i in 0 until 8) {
            sb.append(chars[rnd.nextInt(chars.length)])
        }
        return sb.toString()
    }

    fun generateSalt(): String {
        val chars = "0123456789abcdef"
        val rnd = Random()
        val sb = java.lang.StringBuilder()
        for (i in 0 until 16) {
            sb.append(chars[rnd.nextInt(chars.length)])
        }
        return sb.toString()
    }

    fun hashPasswordWithSalt(password: String, salt: String): String {
        return sha256("$salt:$password")
    }

    fun generateDemoOtp(): String {
        val rnd = Random()
        return (100000 + rnd.nextInt(900000)).toString()
    }

    fun formatIndianCurrency(amount: Double): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            format.maximumFractionDigits = 2
            format.minimumFractionDigits = 2
            format.format(amount)
        } catch (e: Exception) {
            "₹${String.format(Locale.US, "%.2f", amount)}"
        }
    }

    fun computeDualAuthSecondSignature(
        transactionId: String,
        amount: Double,
        recipient: String,
        user1Id: String,
        user2Id: String,
        timestamp: Long,
        nonce: String
    ): String {
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val payload = "DUAL_SIG2:$transactionId:$formattedAmount:${recipient.lowercase()}:${user1Id.lowercase()}:${user2Id.lowercase()}:$timestamp:$nonce"
        return sha256(payload)
    }
}
