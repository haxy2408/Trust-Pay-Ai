package com.example.trustpay.model

import com.google.firebase.firestore.DocumentId

data class PaymentTransaction(
    @DocumentId
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val recipient: String = "",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val status: String = "APPROVED",
    val riskScore: Int = 10,
    val bindingHash: String = "",
    val senderId: String = "",
    val note: String = "",
    val method: String = "P2P_QR",
    val qrTokenId: String? = null
)

data class UserAccount(
    val email: String = "",
    val fullName: String = "",
    val balance: Double = 100000.0,
    val designatedFinger: String = "Right Index Finger",
    val securityLevel: String = "ENHANCED"
)
