package com.example.trustpay.firebase

import com.example.trustpay.model.PaymentTransaction
import com.example.trustpay.model.QrPaymentRequest
import com.example.trustpay.model.QrValidationResult
import com.example.trustpay.model.UserAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()

    val currentUserId: String?
        get() = auth.currentUser?.uid ?: auth.currentUser?.email

    /**
     * Calls Firebase Cloud Function: createQrPaymentRequest
     * Server creates cryptographic token with HMAC-SHA256 signature and stores in Firestore.
     */
    suspend fun createQrPaymentRequest(amount: Double?, note: String): QrPaymentRequest {
        val user = auth.currentUser ?: throw IllegalStateException("User not authenticated")
        val data = hashMapOf(
            "amount" to amount,
            "note" to note,
            "receiverName" to (user.displayName ?: user.email?.substringBefore("@") ?: "Receiver")
        )

        val result = functions
            .getHttpsCallable("createQrPaymentRequest")
            .call(data)
            .await()

        @Suppress("UNCHECKED_CAST")
        val map = result.data as? Map<String, Any> ?: throw IllegalStateException("Invalid response from Cloud Function")
        val tokenMap = map["token"] as? Map<String, Any> ?: throw IllegalStateException("Missing token in response")

        return QrPaymentRequest(
            tokenId = tokenMap["tokenId"] as? String ?: "",
            receiverId = tokenMap["receiverId"] as? String ?: "",
            receiverName = tokenMap["receiverName"] as? String ?: "",
            amount = (tokenMap["amount"] as? Number)?.toDouble(),
            note = tokenMap["note"] as? String ?: "",
            nonce = tokenMap["nonce"] as? String ?: "",
            createdAt = (tokenMap["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            expiresAt = (tokenMap["expiresAt"] as? Number)?.toLong() ?: (System.currentTimeMillis() + 30_000L),
            status = tokenMap["status"] as? String ?: "ACTIVE",
            signature = tokenMap["signature"] as? String ?: "",
            payloadUri = tokenMap["payloadUri"] as? String ?: ""
        )
    }

    /**
     * Calls Firebase Cloud Function: validateQrToken
     * Server checks freshness, nonce, status (USED vs ACTIVE), and validates HMAC signature.
     */
    suspend fun validateQrToken(tokenId: String): QrValidationResult {
        val data = hashMapOf("tokenId" to tokenId)

        return try {
            val result = functions
                .getHttpsCallable("validateQrToken")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val map = result.data as? Map<String, Any> ?: return QrValidationResult(valid = false, error = "Invalid response")
            val isValid = map["valid"] as? Boolean ?: false
            val remaining = (map["remainingSeconds"] as? Number)?.toLong() ?: 0L

            val tokenMap = map["request"] as? Map<String, Any>
            val request = tokenMap?.let {
                QrPaymentRequest(
                    tokenId = it["tokenId"] as? String ?: "",
                    receiverId = it["receiverId"] as? String ?: "",
                    receiverName = it["receiverName"] as? String ?: "",
                    amount = (it["amount"] as? Number)?.toDouble(),
                    note = it["note"] as? String ?: "",
                    nonce = it["nonce"] as? String ?: "",
                    createdAt = (it["createdAt"] as? Number)?.toLong() ?: 0L,
                    expiresAt = (it["expiresAt"] as? Number)?.toLong() ?: 0L,
                    status = it["status"] as? String ?: "UNKNOWN",
                    signature = it["signature"] as? String ?: "",
                    payloadUri = it["payloadUri"] as? String ?: "",
                    usedBySenderId = it["usedBySenderId"] as? String,
                    usedBySenderName = it["usedBySenderName"] as? String,
                    usedAt = (it["usedAt"] as? Number)?.toLong(),
                    transactionId = it["transactionId"] as? String
                )
            }

            QrValidationResult(
                valid = isValid,
                request = request,
                remainingSeconds = remaining,
                error = map["error"] as? String,
                errorCode = map["errorCode"] as? String
            )
        } catch (e: Exception) {
            QrValidationResult(
                valid = false,
                error = e.localizedMessage ?: "Failed to validate QR token",
                errorCode = "NETWORK_ERROR"
            )
        }
    }

    /**
     * Calls Firebase Cloud Function: completeP2PTransfer
     * Executes atomic Firestore transaction to debit sender, credit receiver, and mark token USED.
     */
    suspend fun completeP2PTransfer(
        tokenId: String,
        amount: Double,
        bindingHash: String
    ): PaymentTransaction {
        val user = auth.currentUser ?: throw IllegalStateException("User not authenticated")
        val data = hashMapOf(
            "tokenId" to tokenId,
            "amount" to amount,
            "bindingHash" to bindingHash,
            "senderName" to (user.displayName ?: user.email?.substringBefore("@") ?: "Sender")
        )

        val result = functions
            .getHttpsCallable("completeP2PTransfer")
            .call(data)
            .await()

        @Suppress("UNCHECKED_CAST")
        val map = result.data as? Map<String, Any> ?: throw IllegalStateException("Transfer failed")

        return PaymentTransaction(
            id = map["transactionId"] as? String ?: "",
            timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            recipient = map["receiverName"] as? String ?: "",
            amount = (map["amount"] as? Number)?.toDouble() ?: amount,
            status = map["status"] as? String ?: "APPROVED",
            bindingHash = map["bindingHash"] as? String ?: bindingHash,
            senderId = user.uid,
            note = map["note"] as? String ?: "",
            qrTokenId = tokenId
        )
    }

    /**
     * Real-time listener for the receiving user to detect when their generated QR is paid.
     */
    fun listenToTokenUpdates(
        tokenId: String,
        onUpdate: (QrPaymentRequest) -> Unit
    ): ListenerRegistration {
        return db.collection("p2p_qr_tokens").document(tokenId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val token = snapshot.toObject(QrPaymentRequest::class.java)
                    if (token != null) {
                        onUpdate(token)
                    }
                }
            }
    }

    /**
     * Real-time listener for balance updates.
     */
    fun listenToUserBalance(
        userId: String,
        onBalanceUpdate: (Double) -> Unit
    ): ListenerRegistration {
        return db.collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val bal = snapshot.getDouble("balance") ?: 100000.0
                    onBalanceUpdate(bal)
                }
            }
    }
}
