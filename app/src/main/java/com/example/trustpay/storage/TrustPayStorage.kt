package com.example.trustpay.storage

import com.example.trustpay.model.*
import com.example.trustpay.security.CryptoUtils
import java.util.UUID

object TrustPayStorage {

    const val DEFAULT_BALANCE = 100000.0

    private val users = mutableListOf<UserAccount>()
    private val balances = mutableMapOf<String, Double>()
    private var activeUser: UserAccount? = null

    private val transactions = mutableListOf<PaymentTransaction>()
    private val auditLogs = mutableListOf<AuditLogEntry>()
    private val dualAuthTransactions = mutableListOf<DualAuthTransaction>()
    private val secondSignatureTransactions = mutableListOf<SecondSignatureTransaction>()
    private val coSigners = mutableListOf<CoSigner>()
    private val qrTokens = mutableMapOf<String, QrPaymentRequest>()
    private val locations = mutableListOf<TargetLocation>()

    init {
        seedDefaults()
    }

    private fun seedDefaults() {
        // Seed Default Users
        val salt1 = CryptoUtils.generateSalt()
        val user1 = UserAccount(
            fullName = "Rahul Sharma",
            email = "rahul@example.com",
            mobileNumber = "9876543210",
            passwordHash = CryptoUtils.hashPasswordWithSalt("password123", salt1),
            salt = salt1
        )

        val salt2 = CryptoUtils.generateSalt()
        val user2 = UserAccount(
            fullName = "Priya Patel",
            email = "priya@example.com",
            mobileNumber = "9876543211",
            passwordHash = CryptoUtils.hashPasswordWithSalt("password123", salt2),
            salt = salt2
        )

        users.add(user1)
        users.add(user2)
        activeUser = user1

        balances["rahul@example.com"] = 100000.0
        balances["priya@example.com"] = 65000.0

        // Seed Co-Signers for Dual Authorization
        coSigners.add(
            CoSigner("cosigner_1", "Priya Patel", "priya@example.com", "Chief Financial Officer (CFO)")
        )
        coSigners.add(
            CoSigner("cosigner_2", "Vikram Mehta", "vikram@example.com", "Risk & Compliance Officer")
        )
        coSigners.add(
            CoSigner("cosigner_3", "Ananya Rao", "ananya@example.com", "Senior Treasury Director")
        )

        // Seed Default Locations (Mumbai / Metro area landmarks)
        locations.add(
            TargetLocation("loc_1", "HDFC Bank BKC Premier Branch", "Bank Branch", "Bandra Kurla Complex, Mumbai", 19.0674, 72.8687, "Designated premier commercial branch")
        )
        locations.add(
            TargetLocation("loc_2", "State Bank Smart ATM Kiosk", "ATM", "Kurla West, Mumbai", 19.0728, 72.8795, "24/7 Biometric-enabled ATM")
        )
        locations.add(
            TargetLocation("loc_3", "Reliance Digital Flagship", "Merchant", "Phoenix Mall, Lower Parel", 18.9953, 72.8258, "Verified retail partner store")
        )
        locations.add(
            TargetLocation("loc_4", "Tata Croma Electronic Store", "Merchant", "Andheri East, Mumbai", 19.1136, 72.8697, "Registered high-value merchant")
        )
        locations.add(
            TargetLocation("loc_5", "Chhatrapati Shivaji Intl Airport Duty Free", "Airport", "Terminal 2, Mumbai", 19.0896, 72.8656, "High-security terminal concession")
        )
        locations.add(
            TargetLocation("loc_6", "Navi Mumbai Tech Park Merchant", "Remote Hub", "Vashi, Navi Mumbai", 19.0771, 72.9986, "Suburban payment destination")
        )
        locations.add(
            TargetLocation("loc_7", "Thane Supercenter Outlet", "Retail Hub", "Ghubunder Road, Thane", 19.2612, 72.9645, "Outer metro perimeter partner")
        )

        // Initial System Audit Log
        val genesisHash = CryptoUtils.sha256("TRUSTPAY_GENESIS_SECURITY_STATE")
        auditLogs.add(
            AuditLogEntry(
                id = "LOG_INIT_001",
                timestamp = System.currentTimeMillis() - 60000,
                title = "Security Architecture Online",
                details = "SHA-256 parameter binding and anomaly detection engine initialized.",
                status = AuditStatus.SUCCESS,
                hash = genesisHash
            )
        )

        // Seed Initial Second Signature Transaction (High-Risk Enterprise Transfer)
        val seedTxId = "TXN_2ND_CORP88"
        val seedNonce = "NONCE_INIT_901"
        val seedCreatedAt = System.currentTimeMillis() - 30 * 1000L
        val seedFingerprint = com.example.trustpay.security.SecondSignatureEngine.computeTransactionFingerprint(
            transactionId = seedTxId,
            senderId = "rahul@example.com",
            recipientId = "infra.cloud@icicibank",
            amount = 85000.0,
            currency = "INR",
            createdAt = seedCreatedAt,
            transactionType = "CORPORATE_PAYMENT",
            transactionVersion = 1,
            nonce = seedNonce
        )
        val seedFirstSig = com.example.trustpay.security.SecondSignatureEngine.generateSignerSignature(
            signerId = "rahul@example.com",
            signerName = "Rahul Sharma",
            signerRole = "Initiator / Primary Signer",
            transactionId = seedTxId,
            transactionFingerprint = seedFingerprint,
            transactionVersion = 1,
            timestamp = seedCreatedAt,
            nonce = seedNonce
        )
        secondSignatureTransactions.add(
            SecondSignatureTransaction(
                id = seedTxId,
                senderId = "rahul@example.com",
                senderName = "Rahul Sharma",
                recipientId = "infra.cloud@icicibank",
                amount = 85000.0,
                currency = "INR",
                transactionType = "CORPORATE_PAYMENT",
                purposeDescription = "Multi-Region Cloud Infrastructure Expansion",
                status = SecondSignatureStatus.SECOND_SIGNATURE_REQUIRED,
                transactionVersion = 1,
                transactionFingerprint = seedFingerprint,
                createdAt = seedCreatedAt,
                expiresAt = seedCreatedAt + com.example.trustpay.security.SecondSignatureEngine.SECOND_SIGNATURE_EXPIRY_MS,
                designatedSecondSignerId = "priya@example.com",
                designatedSecondSignerName = "Priya Patel",
                designatedSecondSignerRole = "Chief Financial Officer (CFO)",
                firstSignature = seedFirstSig,
                riskScore = 65,
                nonce = seedNonce
            )
        )
    }

    // User Operations
    fun getUsers(): List<UserAccount> = users.toList()

    fun getActiveUser(): UserAccount? = activeUser

    fun setActiveUser(user: UserAccount?) {
        activeUser = user
        if (user != null) {
            addAuditLog(
                "Switched Active User",
                "Session shifted to ${user.fullName} (${user.email}).",
                AuditStatus.INFO
            )
        }
    }

    fun registerUser(user: UserAccount): Boolean {
        if (users.any { it.email.equals(user.email, ignoreCase = true) || it.mobileNumber == user.mobileNumber }) {
            return false
        }
        users.add(user)
        balances[user.email] = DEFAULT_BALANCE
        setActiveUser(user)
        addAuditLog(
            "New Account Enrolled",
            "User ${user.fullName} (${user.email}) registered with biometric baseline.",
            AuditStatus.SUCCESS
        )
        return true
    }

    // Balance Operations
    fun getBalance(email: String? = null): Double {
        val target = email ?: activeUser?.email ?: "default"
        return balances[target] ?: DEFAULT_BALANCE
    }

    fun setBalance(newBalance: Double, email: String? = null) {
        val target = email ?: activeUser?.email ?: "default"
        balances[target] = newBalance
    }

    fun resetBalance(newBalance: Double = DEFAULT_BALANCE) {
        val email = activeUser?.email ?: "default"
        balances[email] = newBalance
        addAuditLog(
            "Account Balance Reset",
            "Demo liquidity reset to ${CryptoUtils.formatIndianCurrency(newBalance)}.",
            AuditStatus.INFO
        )
    }

    // Transactions
    fun getTransactions(): List<PaymentTransaction> = transactions.toList().sortedByDescending { it.timestamp }

    fun addTransaction(tx: PaymentTransaction) {
        transactions.add(0, tx)
    }

    fun getRollingWindowTransactions(windowMinutes: Int = 5): List<PaymentTransaction> {
        val cutoff = System.currentTimeMillis() - windowMinutes * 60 * 1000L
        return transactions.filter {
            it.status == "APPROVED" && it.timestamp >= cutoff
        }
    }

    fun getRollingWindowTotalAmount(windowMinutes: Int = 5): Double {
        return getRollingWindowTransactions(windowMinutes).sumOf { it.amount }
    }

    // Audit Logs
    fun getAuditLogs(): List<AuditLogEntry> = auditLogs.toList().sortedByDescending { it.timestamp }

    fun addAuditLog(title: String, details: String, status: AuditStatus, hash: String? = null) {
        val entry = AuditLogEntry(
            id = "LOG_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
            timestamp = System.currentTimeMillis(),
            title = title,
            details = details,
            status = status,
            hash = hash
        )
        auditLogs.add(0, entry)
    }

    // Dual Authorization
    fun getCoSigners(): List<CoSigner> = coSigners.filter { it.active }

    fun getDualAuthTransactions(): List<DualAuthTransaction> {
        val now = System.currentTimeMillis()
        // Auto-expire
        dualAuthTransactions.forEachIndexed { index, tx ->
            if (tx.status == DualAuthStatus.PENDING_SECOND_AUTH && now > tx.expiresAt) {
                dualAuthTransactions[index] = tx.copy(
                    status = DualAuthStatus.EXPIRED,
                    rejectionReason = "5-minute approval window elapsed."
                )
            }
        }
        return dualAuthTransactions.toList().sortedByDescending { it.createdAt }
    }

    fun addDualAuthTransaction(tx: DualAuthTransaction) {
        dualAuthTransactions.add(0, tx)
        addAuditLog(
            "Dual-Auth Requested",
            "Payment of ${CryptoUtils.formatIndianCurrency(tx.amount)} requires second signature from ${tx.coSignerName}.",
            AuditStatus.WARNING,
            tx.firstApprovalSignature
        )
    }

    fun approveDualAuthTransaction(
        txId: String,
        coSignerEmail: String,
        coSignerName: String
    ): Boolean {
        val index = dualAuthTransactions.indexOfFirst { it.id == txId }
        if (index == -1) return false
        val tx = dualAuthTransactions[index]

        // Rule 1: User 1 cannot approve as User 2
        if (coSignerEmail.equals(tx.initiatorId, ignoreCase = true)) {
            addAuditLog(
                "Security Policy Violation",
                "User 1 (${tx.initiatorName}) attempted self-approval as User 2.",
                AuditStatus.DANGER
            )
            return false
        }

        // Rule 2: Expired?
        if (tx.isExpired()) {
            dualAuthTransactions[index] = tx.copy(status = DualAuthStatus.EXPIRED)
            return false
        }

        val initiatorBal = getBalance(tx.initiatorId)
        if (initiatorBal < tx.amount) {
            dualAuthTransactions[index] = tx.copy(
                status = DualAuthStatus.BLOCKED,
                rejectionReason = "Initiator has insufficient funds."
            )
            return false
        }

        // Deduct
        setBalance(initiatorBal - tx.amount, tx.initiatorId)
        val now = System.currentTimeMillis()
        val sig2 = CryptoUtils.computeDualAuthSecondSignature(
            tx.id, tx.amount, tx.recipient, tx.initiatorId, coSignerEmail, now, tx.nonce
        )

        val updated = tx.copy(
            status = DualAuthStatus.APPROVED_BY_TWO_SIGNERS,
            secondApprovalSignature = sig2,
            secondApprovedAt = now,
            approvedByUserId = coSignerEmail,
            approvedByUserName = coSignerName,
            balanceDeducted = true
        )
        dualAuthTransactions[index] = updated

        // Also add to primary transaction ledger
        addTransaction(
            PaymentTransaction(
                id = tx.id,
                recipient = tx.recipient,
                amount = tx.amount,
                status = "APPROVED",
                bindingHash = sig2,
                riskScore = tx.riskScore,
                senderId = tx.initiatorId,
                note = "[Dual-Auth Approved] ${tx.note}",
                balanceAfter = getBalance(tx.initiatorId)
            )
        )

        addAuditLog(
            "Dual-Auth Payment Executed",
            "Payment of ${CryptoUtils.formatIndianCurrency(tx.amount)} to ${tx.recipient} approved by $coSignerName.",
            AuditStatus.SUCCESS,
            sig2
        )
        return true
    }

    fun rejectDualAuthTransaction(txId: String, reason: String): Boolean {
        val index = dualAuthTransactions.indexOfFirst { it.id == txId }
        if (index == -1) return false
        val tx = dualAuthTransactions[index]
        dualAuthTransactions[index] = tx.copy(
            status = DualAuthStatus.REJECTED_BY_SECOND_SIGNER,
            rejectionReason = reason
        )
        addAuditLog(
            "Dual-Auth Rejected",
            "Payment of ${CryptoUtils.formatIndianCurrency(tx.amount)} rejected: $reason",
            AuditStatus.DANGER
        )
        return true
    }

    // QR Tokens
    fun getQrTokens(): List<QrPaymentRequest> = qrTokens.values.toList().sortedByDescending { it.createdAt }

    fun saveQrToken(token: QrPaymentRequest) {
        qrTokens[token.tokenId] = token
    }

    fun getQrToken(tokenId: String): QrPaymentRequest? = qrTokens[tokenId]

    // Second Signature Transactions
    fun getSecondSignatureTransactions(): List<SecondSignatureTransaction> {
        val now = System.currentTimeMillis()
        secondSignatureTransactions.forEachIndexed { index, tx ->
            if ((tx.status == SecondSignatureStatus.SECOND_SIGNATURE_REQUIRED ||
                        tx.status == SecondSignatureStatus.PENDING) && now > tx.expiresAt) {
                secondSignatureTransactions[index] = tx.copy(
                    status = SecondSignatureStatus.EXPIRED,
                    failureReason = "10-minute approval window elapsed."
                )
            }
        }
        return secondSignatureTransactions.toList().sortedByDescending { it.createdAt }
    }

    fun getSecondSignatureTransaction(id: String): SecondSignatureTransaction? {
        return secondSignatureTransactions.firstOrNull { it.id == id }
    }

    fun addSecondSignatureTransaction(tx: SecondSignatureTransaction) {
        secondSignatureTransactions.add(0, tx)
    }

    fun updateSecondSignatureTransaction(tx: SecondSignatureTransaction) {
        val index = secondSignatureTransactions.indexOfFirst { it.id == tx.id }
        if (index != -1) {
            secondSignatureTransactions[index] = tx
        } else {
            secondSignatureTransactions.add(0, tx)
        }
    }

    // Locations
    fun getLocations(): List<TargetLocation> = locations.toList()

    fun addLocation(location: TargetLocation) {
        locations.add(0, location)
    }
}
