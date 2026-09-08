package com.example.trustpay.model

data class UserAccount(
    val fullName: String,
    val email: String,
    val mobileNumber: String,
    val passwordHash: String,
    val salt: String,
    val registeredAt: Long = System.currentTimeMillis()
)

data class PaymentTransaction(
    val id: String,
    val recipient: String,
    val amount: Double,
    val currency: String = "INR",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "APPROVED", // "APPROVED", "BLOCKED", "TAMPER_DETECTED", "PENDING"
    val bindingHash: String = "",
    val riskScore: Int = 10,
    val balanceAfter: Double? = null,
    val senderId: String = "",
    val note: String = "",
    val method: String = "UPI_BINDING",
    val qrTokenId: String? = null
)

data class RiskAnalysisResult(
    val score: Int,
    val signals: List<String>,
    val requiresBiometric: Boolean,
    val requiresOtp: Boolean,
    val isVelocityAnomaly: Boolean,
    val boundHash: String,
    val aiInsights: String
)

data class AuthenticatedTransactionSnapshot(
    val transactionId: String,
    val recipient: String,
    val amount: Double,
    val currency: String = "INR",
    val bindingHash: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class FinalDecisionStatus {
    NONE, APPROVED, TAMPER_DETECTED, BLOCKED
}

enum class VerificationStep {
    NONE, BIOMETRIC, CHALLENGE, DEMO_OTP, COMPLETED
}

data class ChallengeData(
    val question: String,
    val expectedAnswer: String
)

data class VerificationState(
    val transactionId: String,
    val recipient: String,
    val amount: Double,
    val currentStep: VerificationStep = VerificationStep.NONE,
    val isVelocityAnomaly: Boolean = false,
    val designatedFinger: String = "Right Index Finger",
    val challengeData: ChallengeData? = null,
    val demoOtp: String = "123456"
)

enum class AuditStatus {
    SUCCESS, WARNING, DANGER, INFO
}

data class AuditLogEntry(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val details: String,
    val status: AuditStatus,
    val hash: String? = null
)

enum class QrStatus {
    ACTIVE, EXPIRED, USED, REFRESHING
}

data class QrPaymentRequest(
    val tokenId: String,
    val receiverId: String,
    val receiverName: String,
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

data class CoSigner(
    val id: String,
    val fullName: String,
    val email: String,
    val role: String,
    val active: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)

enum class DualAuthStatus {
    PENDING_SECOND_AUTH,
    APPROVED_BY_TWO_SIGNERS,
    REJECTED_BY_SECOND_SIGNER,
    EXPIRED,
    TAMPER_DETECTED,
    BLOCKED
}

data class DualAuthTransaction(
    val id: String,
    val initiatorId: String,
    val initiatorName: String,
    val coSignerId: String,
    val coSignerName: String,
    val recipient: String,
    val amount: Double,
    val currency: String = "INR",
    val note: String = "Dual-Authorization Transfer",
    val status: DualAuthStatus = DualAuthStatus.PENDING_SECOND_AUTH,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 5 * 60 * 1000L, // 5 minutes TTL
    val riskScore: Int = 20,
    val signals: List<String> = emptyList(),
    val nonce: String = "",
    val firstApprovalSignature: String = "",
    val secondApprovalSignature: String? = null,
    val secondApprovedAt: Long? = null,
    val approvedByUserId: String? = null,
    val approvedByUserName: String? = null,
    val rejectionReason: String? = null,
    val balanceDeducted: Boolean = false
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    fun getRemainingSeconds(): Long {
        val diff = (expiresAt - System.currentTimeMillis()) / 1000
        return if (diff > 0) diff else 0
    }
}

enum class DistanceCategory(val label: String) {
    USUAL("Usual distance"),
    MEDIUM("Medium distance"),
    FAR("Far distance"),
    HIGH("High distance")
}

data class TargetLocation(
    val id: String,
    val name: String,
    val category: String = "Branch",
    val address: String = "",
    val latitude: Double,
    val longitude: Double,
    val description: String = ""
)

data class ClassifiedLocation(
    val location: TargetLocation,
    val distanceKm: Double,
    val formattedDistance: String,
    val displayLabel: String,
    val category: DistanceCategory,
    val colorIndicator: String // "GREEN", "YELLOW", "ORANGE", "RED"
)
