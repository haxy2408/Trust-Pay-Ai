package com.example.trustpay.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class QrStatus {
    ACTIVE,
    EXPIRED,
    USED,
    REFRESHING
}

data class QrPaymentRequest(
    @DocumentId
    val tokenId: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val amount: Double? = null,
    val note: String = "",
    val nonce: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 30_000L,
    val status: String = QrStatus.ACTIVE.name,
    val signature: String = "",
    val payloadUri: String = "",
    val usedBySenderId: String? = null,
    val usedBySenderName: String? = null,
    val usedAt: Long? = null,
    val transactionId: String? = null
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    fun isAvailable(): Boolean = status == QrStatus.ACTIVE.name && !isExpired()
    fun getRemainingSeconds(): Long {
        val diff = (expiresAt - System.currentTimeMillis()) / 1000
        return if (diff > 0) diff else 0
    }
}

data class QrValidationResult(
    val valid: Boolean = false,
    val request: QrPaymentRequest? = null,
    val remainingSeconds: Long = 0,
    val error: String? = null,
    val errorCode: String? = null
)
