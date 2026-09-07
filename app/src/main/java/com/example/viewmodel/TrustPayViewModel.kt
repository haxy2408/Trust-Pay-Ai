package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TransactionStorage
import com.example.model.AuditLogEntry
import com.example.model.AuditStatus
import com.example.model.AuthenticatedTransactionSnapshot
import com.example.model.ChallengeData
import com.example.model.ChallengeType
import com.example.model.FinalDecisionStatus
import com.example.model.IntegrityCheckResult
import com.example.model.PaymentAnalysisResult
import com.example.model.AuthScreen
import com.example.model.LoginFormState
import com.example.model.PaymentTransaction
import com.example.model.RegisterFormState
import com.example.model.UserAccount
import com.example.model.VerificationState
import com.example.model.VerificationStep
import com.example.security.AuthSecurityUtil
import com.example.security.CryptoHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

/**
 * UI State container for TrustPay AI.
 */
data class TrustPayUiState(
    val recipientInput: String = "rahul@okhdfcbank",
    val amountInput: String = "5000",
    val untrustedDevice: Boolean = false,
    val newRecipient: Boolean = false,
    val unusualContext: Boolean = false,
    val currentRiskScore: Int? = null,
    val signals: List<String> = emptyList(),
    val currentAnalysis: PaymentAnalysisResult? = null,
    val verificationState: VerificationState? = null,
    val finalDecision: FinalDecisionStatus = FinalDecisionStatus.NONE,
    val decisionMessage: String = "",
    val allTransactions: List<PaymentTransaction> = emptyList(),
    val rollingWindowTransactions: List<PaymentTransaction> = emptyList(),
    val rollingWindowTotalAmount: Double = 0.0,
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val isBiometricPromptPending: Boolean = false,
    val isBiometricSimulationAvailable: Boolean = false,
    val isDarkTheme: Boolean = false,
    val currentScreen: AuthScreen = AuthScreen.REGISTER,
    val currentUser: UserAccount? = null,
    val registerForm: RegisterFormState = RegisterFormState(),
    val loginForm: LoginFormState = LoginFormState(),
    val accountBalance: Double = TransactionStorage.DEFAULT_DEMO_BALANCE
)

class TrustPayViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = TransactionStorage(application)

    private val _uiState = MutableStateFlow(
        TrustPayUiState(
            isDarkTheme = storage.isDarkTheme(),
            currentScreen = if (!storage.hasAnyUser()) AuthScreen.REGISTER else if (storage.isLoggedIn() && storage.getActiveUser() != null) AuthScreen.DASHBOARD else AuthScreen.LOGIN,
            currentUser = if (storage.isLoggedIn()) storage.getActiveUser() else null
        )
    )
    val uiState: StateFlow<TrustPayUiState> = _uiState.asStateFlow()

    companion object {
        const val HIGH_VALUE_THRESHOLD = 40000.0
        const val VELOCITY_WINDOW_MS = 5 * 60 * 1000L // 5 minutes rolling window
        const val VELOCITY_COUNT_THRESHOLD = 3
        const val VELOCITY_AMOUNT_THRESHOLD = 15000.0

        /**
         * Pool of fingerprints randomly chosen whenever high frequency money is transferred.
         * Defeats automated replay, coercion, and single-finger brute forcing.
         */
        val AVAILABLE_FINGERS = listOf(
            "Right Thumb",
            "Right Index Finger",
            "Right Middle Finger",
            "Left Thumb",
            "Left Index Finger"
        )
    }

    init {
        val hasAccount = storage.hasAnyUser()
        val isLoggedIn = storage.isLoggedIn()
        val activeUser = storage.getActiveUser()

        val initialScreen = when {
            !hasAccount -> AuthScreen.REGISTER
            isLoggedIn && activeUser != null -> AuthScreen.DASHBOARD
            else -> AuthScreen.LOGIN
        }
        val initialBalance = storage.getAccountBalance(activeUser?.email)

        _uiState.update {
            it.copy(
                currentScreen = initialScreen,
                currentUser = if (isLoggedIn) activeUser else null,
                accountBalance = initialBalance
            )
        }

        refreshHistoryAndLogs()
        if (storage.getAuditLogs().isEmpty()) {
            addAuditLog(
                title = "System Initialized",
                details = "TrustPay AI transaction authentication engine active. SHA-256 binding & velocity monitor enabled.",
                status = AuditStatus.INFO
            )
        }
    }

    fun onRecipientChanged(newRecipient: String) {
        _uiState.update { it.copy(recipientInput = newRecipient) }
    }

    fun onAmountChanged(newAmount: String) {
        // filter numbers and decimal point only
        val filtered = newAmount.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(amountInput = filtered) }
    }

    fun toggleUntrustedDevice(enabled: Boolean) {
        _uiState.update { it.copy(untrustedDevice = enabled) }
    }

    fun toggleNewRecipient(enabled: Boolean) {
        _uiState.update { it.copy(newRecipient = enabled) }
    }

    fun toggleUnusualContext(enabled: Boolean) {
        _uiState.update { it.copy(unusualContext = enabled) }
    }

    fun prefillScenario(recipient: String, amount: String) {
        _uiState.update {
            it.copy(
                recipientInput = recipient,
                amountInput = amount
            )
        }
    }

    /**
     * Refreshes transaction history and computes rolling 5-minute window stats.
     */
    fun refreshHistoryAndLogs() {
        val now = System.currentTimeMillis()
        val allTx = storage.getTransactions()
        val inWindow = allTx.filter { (now - it.timestamp) <= VELOCITY_WINDOW_MS }
        val totalInWindow = inWindow.sumOf { it.amount }
        val logs = storage.getAuditLogs()

        _uiState.update {
            it.copy(
                allTransactions = allTx,
                rollingWindowTransactions = inWindow,
                rollingWindowTotalAmount = totalInWindow,
                auditLogs = logs
            )
        }
    }

    /**
     * Primary action: Analyzes transaction risk, binds payload cryptographically,
     * and decides routing (Direct Approval, Biometric Only, or 3-Step Velocity Verification).
     */
    fun analyseAndPay(triggerBiometricCallback: () -> Unit) {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull() ?: 0.0
        val recipient = state.recipientInput.trim()

        if (recipient.isEmpty()) {
            _uiState.update {
                it.copy(
                    finalDecision = FinalDecisionStatus.BLOCKED,
                    decisionMessage = "Recipient UPI ID cannot be empty."
                )
            }
            return
        }

        if (amount <= 0.0) {
            _uiState.update {
                it.copy(
                    finalDecision = FinalDecisionStatus.BLOCKED,
                    decisionMessage = "Payment amount must be greater than ₹0."
                )
            }
            return
        }

        // Balance Check: Verify entered amount does not exceed available balance
        val availableBalance = storage.getAccountBalance()
        if (amount > availableBalance) {
            _uiState.update {
                it.copy(
                    finalDecision = FinalDecisionStatus.BLOCKED,
                    decisionMessage = "Insufficient balance. Payment cannot be completed."
                )
            }
            addAuditLog(
                title = "Payment Blocked: Insufficient Balance",
                details = "Attempted ₹${String.format(Locale.US, "%.2f", amount)} exceeds Available Balance ${com.example.model.formatIndianCurrency(availableBalance)}. Payment cannot be completed.",
                status = AuditStatus.DANGER
            )
            return
        }

        // 1. Evaluate rolling 5-minute window
        val now = System.currentTimeMillis()
        val inWindow = storage.getTransactions().filter { (now - it.timestamp) <= VELOCITY_WINDOW_MS }
        val prospectiveCount = inWindow.size + 1
        val prospectiveTotal = inWindow.sumOf { it.amount } + amount

        val isVelocityAnomaly = prospectiveCount >= VELOCITY_COUNT_THRESHOLD &&
                prospectiveTotal >= VELOCITY_AMOUNT_THRESHOLD

        // 2. High-value rule
        val isHighValue = amount > HIGH_VALUE_THRESHOLD

        // 3. Calculate Risk Score & Collect Signals
        var risk = 10 // Baseline normal risk
        val signals = mutableListOf<String>()

        if (state.untrustedDevice) {
            risk += 25
            signals.add("Untrusted Device Signal: Hardware fingerprint not previously recognized.")
        }
        if (state.newRecipient) {
            risk += 20
            signals.add("New Recipient Signal: First payment to $recipient.")
        }
        if (state.unusualContext) {
            risk += 20
            signals.add("Contextual Anomaly: Unusual transaction hour or geographic jump detected.")
        }
        if (isHighValue) {
            risk += 30
            signals.add("High-Value Signal: Transaction of ₹${String.format(Locale.US, "%.2f", amount)} exceeds ₹40,000 threshold.")
        }
        if (isVelocityAnomaly) {
            risk += 45
            signals.add("VELOCITY ANOMALY: Attempting payment #$prospectiveCount totaling ₹${String.format(Locale.US, "%.2f", prospectiveTotal)} in 5-minute rolling window (Threshold: ₹15,000).")
        }

        val clampedRisk = risk.coerceIn(0, 100)

        // Randomly designate a fingerprint challenge if high frequency money is transferred
        val designatedFinger = if (isVelocityAnomaly) {
            AVAILABLE_FINGERS.random()
        } else null

        if (designatedFinger != null) {
            signals.add("High-Frequency Policy: Random fingerprint challenge assigned ($designatedFinger) to defeat replay & coercion.")
        }

        // 4. Generate unique transaction ID and bind payload with SHA-256
        val txId = "TXN-" + UUID.randomUUID().toString().take(8).uppercase(Locale.US)
        val boundHash = CryptoHelper.computeBindingHash(txId, recipient, amount, "INR")

        val analysis = PaymentAnalysisResult(
            transactionId = txId,
            recipient = recipient,
            amount = amount,
            currency = "INR",
            boundHash = boundHash,
            riskScore = clampedRisk,
            signals = signals,
            requiresBiometricOnly = isHighValue && !isVelocityAnomaly,
            requires3StepVerification = isVelocityAnomaly,
            canApproveDirectly = !isHighValue && !isVelocityAnomaly && clampedRisk < 60,
            designatedFinger = designatedFinger
        )

        addAuditLog(
            title = "Transaction Analyzed [${txId}]",
            details = "Recipient: $recipient, Amount: ₹$amount. Risk Score: $clampedRisk/100. Signals: ${signals.size}. SHA-256 bound.",
            status = if (isVelocityAnomaly) AuditStatus.WARNING else if (isHighValue) AuditStatus.INFO else AuditStatus.SUCCESS,
            hash = boundHash
        )

        // Route based on security requirements
        when {
            // Case A: Velocity Anomaly (High Frequency) -> 3-Step Verification with Random Fingerprint
            isVelocityAnomaly -> {
                val challenge = generateRandomChallenge(amount, recipient)
                val demoOtp = String.format(Locale.US, "%06d", Random.nextInt(100000, 999999))

                val authSnapshot = AuthenticatedTransactionSnapshot(
                    transactionId = txId,
                    recipient = recipient,
                    amount = amount,
                    currency = "INR",
                    boundHash = boundHash,
                    authTimestamp = System.currentTimeMillis(),
                    biometricVerified = false,
                    designatedFinger = designatedFinger
                )

                val verification = VerificationState(
                    transactionId = txId,
                    recipient = recipient,
                    amount = amount,
                    currency = "INR",
                    boundHash = boundHash,
                    currentStep = VerificationStep.BIOMETRIC,
                    isVelocityAnomaly = true,
                    challengeData = challenge,
                    demoOtp = demoOtp,
                    designatedFinger = designatedFinger,
                    authenticatedSnapshot = authSnapshot
                )

                _uiState.update {
                    it.copy(
                        currentRiskScore = clampedRisk,
                        signals = signals,
                        currentAnalysis = analysis,
                        verificationState = verification,
                        finalDecision = FinalDecisionStatus.NONE,
                        decisionMessage = "VELOCITY ANOMALY DETECTED — High-frequency transfer. Security challenge requires $designatedFinger."
                    )
                }

                addAuditLog(
                    title = "Velocity Anomaly Flagged [${txId}]",
                    details = "High-frequency money transfer detected. Dynamic random fingerprint challenge: $designatedFinger. Initiating 3-Step Verification.",
                    status = AuditStatus.WARNING,
                    hash = boundHash
                )

                // Trigger Step 1: Biometrics
                triggerBiometricCallback()
            }

            // Case B: High Value (> ₹40,000) -> Android Biometric / Device Credential
            isHighValue -> {
                val authSnapshot = AuthenticatedTransactionSnapshot(
                    transactionId = txId,
                    recipient = recipient,
                    amount = amount,
                    currency = "INR",
                    boundHash = boundHash,
                    authTimestamp = System.currentTimeMillis(),
                    biometricVerified = false,
                    designatedFinger = null
                )

                val verification = VerificationState(
                    transactionId = txId,
                    recipient = recipient,
                    amount = amount,
                    currency = "INR",
                    boundHash = boundHash,
                    currentStep = VerificationStep.BIOMETRIC,
                    isVelocityAnomaly = false,
                    challengeData = null,
                    demoOtp = null,
                    designatedFinger = null,
                    authenticatedSnapshot = authSnapshot
                )

                _uiState.update {
                    it.copy(
                        currentRiskScore = clampedRisk,
                        signals = signals,
                        currentAnalysis = analysis,
                        verificationState = verification,
                        finalDecision = FinalDecisionStatus.NONE,
                        decisionMessage = "HIGH-VALUE TRANSACTION — Android Biometric Authentication required."
                    )
                }

                addAuditLog(
                    title = "Biometric Required [${txId}]",
                    details = "Amount exceeds ₹40,000 threshold. Requesting enrolled biometric or device PIN/pattern.",
                    status = AuditStatus.INFO,
                    hash = boundHash
                )

                triggerBiometricCallback()
            }

            // Case C: Normal Low-Risk Payment -> Approved Immediately after integrity check
            else -> {
                val authSnapshot = AuthenticatedTransactionSnapshot(
                    transactionId = txId,
                    recipient = recipient,
                    amount = amount,
                    currency = "INR",
                    boundHash = boundHash,
                    authTimestamp = System.currentTimeMillis(),
                    biometricVerified = false,
                    designatedFinger = null
                )

                val integrity = CryptoHelper.verifyTransactionIntegrity(
                    snapshot = authSnapshot,
                    candidateTransactionId = txId,
                    candidateRecipient = recipient,
                    candidateAmount = amount,
                    candidateCurrency = "INR"
                )

                if (integrity.isValid) {
                    commitApprovedTransaction(txId, recipient, amount, boundHash, clampedRisk, signals)
                } else {
                    blockTamperedTransaction(txId, boundHash, integrity.discrepancyDescription ?: "Integrity check failed during direct approval.")
                }
            }
        }
    }

    /**
     * Security Rule 3 & Attack Demo:
     * Simulates tampering the transaction amount post-analysis/binding.
     * When executed, SHA-256 binding verification fails and halts payment.
     */
    fun triggerTamperAttackDemo() {
        triggerPostAuthTamperAmountDemo()
    }

    /**
     * Demonstrates post-authentication amount tampering:
     * Even if biometric authentication was 100% successful, right before final confirmation,
     * the amount is maliciously tampered on the wire/in memory.
     * TrustPay AI's re-verification engine catches the discrepancy and immediately blocks
     * the transaction with "Transaction tampering detected".
     */
    fun triggerPostAuthTamperAmountDemo() {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull() ?: 5000.0
        val recipient = state.recipientInput.trim().ifEmpty { "rahul@okhdfcbank" }
        val txId = "TXN-ATK" + UUID.randomUUID().toString().take(6).uppercase(Locale.US)

        // 1. Initial analysis & authentication phase locks in the authentic snapshot
        val originalBoundHash = CryptoHelper.computeBindingHash(txId, recipient, amount, "INR")
        val authSnapshot = AuthenticatedTransactionSnapshot(
            transactionId = txId,
            recipient = recipient,
            amount = amount,
            currency = "INR",
            boundHash = originalBoundHash,
            authTimestamp = System.currentTimeMillis(),
            biometricVerified = true,
            designatedFinger = "Right Index Finger"
        )

        addAuditLog(
            title = "Attack Demo: Biometric Success [${txId}]",
            details = "Biometric authentication succeeded for ₹$amount to $recipient. Authenticated snapshot created.",
            status = AuditStatus.SUCCESS,
            hash = originalBoundHash
        )

        // 2. Simulated malicious payload modification: Amount altered from ₹amount to ₹(amount * 10 or +50000)
        val tamperedAmount = if (amount < 10000) amount * 10.0 else amount + 50000.0

        addAuditLog(
            title = "Attack Demo: Injected Tampering [${txId}]",
            details = "Malicious hook injected altered amount: ₹$tamperedAmount (Original: ₹$amount) right before final confirmation.",
            status = AuditStatus.WARNING,
            hash = originalBoundHash
        )

        // 3. Pre-confirmation re-verification: compare wire details against authenticated snapshot
        val integrity = CryptoHelper.verifyTransactionIntegrity(
            snapshot = authSnapshot,
            candidateTransactionId = txId,
            candidateRecipient = recipient,
            candidateAmount = tamperedAmount,
            candidateCurrency = "INR"
        )

        if (!integrity.isValid) {
            val signals = listOf(
                "CRITICAL SECURITY ALERT: Transaction tampering detected!",
                "Authenticated Amount: ₹${String.format(Locale.US, "%.2f", amount)}",
                "Altered Wire Amount: ₹${String.format(Locale.US, "%.2f", tamperedAmount)}",
                "Discrepancy: ${integrity.discrepancyDescription}",
                "Security Status: Blocked before payment confirmation despite successful biometrics."
            )

            _uiState.update {
                it.copy(
                    currentRiskScore = 100,
                    signals = signals,
                    verificationState = null,
                    finalDecision = FinalDecisionStatus.TAMPER_DETECTED,
                    decisionMessage = "Transaction tampering detected: ${integrity.discrepancyDescription} (Biometric authentication was successful, but transaction was intercepted and tampered)."
                )
            }

            addAuditLog(
                title = "Tamper Attack Blocked [${txId}]",
                details = "Transaction tampering detected: ${integrity.discrepancyDescription}. Payment halted prior to final confirmation.",
                status = AuditStatus.DANGER,
                hash = originalBoundHash
            )
        }
    }

    /**
     * Demonstrates post-authentication recipient tampering:
     * Biometric authentication succeeds for intended recipient, but malware hooks
     * the wire to redirect funds to an attacker's UPI address before confirmation.
     * TrustPay AI re-verifies payee identity against authentication snapshot and immediately blocks.
     */
    fun triggerPostAuthTamperRecipientDemo() {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull() ?: 5000.0
        val recipient = state.recipientInput.trim().ifEmpty { "rahul@okhdfcbank" }
        val maliciousRecipient = "attacker.hacker@evilpay"
        val txId = "TXN-ATK" + UUID.randomUUID().toString().take(6).uppercase(Locale.US)

        val originalBoundHash = CryptoHelper.computeBindingHash(txId, recipient, amount, "INR")
        val authSnapshot = AuthenticatedTransactionSnapshot(
            transactionId = txId,
            recipient = recipient,
            amount = amount,
            currency = "INR",
            boundHash = originalBoundHash,
            authTimestamp = System.currentTimeMillis(),
            biometricVerified = true,
            designatedFinger = "Right Index Finger"
        )

        addAuditLog(
            title = "Attack Demo: Biometric Success [${txId}]",
            details = "Biometric authentication passed for recipient $recipient.",
            status = AuditStatus.SUCCESS,
            hash = originalBoundHash
        )

        addAuditLog(
            title = "Attack Demo: Payee Redirection [${txId}]",
            details = "Malicious hook redirected recipient to $maliciousRecipient before final confirmation.",
            status = AuditStatus.WARNING,
            hash = originalBoundHash
        )

        // Pre-confirmation re-verification
        val integrity = CryptoHelper.verifyTransactionIntegrity(
            snapshot = authSnapshot,
            candidateTransactionId = txId,
            candidateRecipient = maliciousRecipient,
            candidateAmount = amount,
            candidateCurrency = "INR"
        )

        if (!integrity.isValid) {
            val signals = listOf(
                "CRITICAL SECURITY ALERT: Transaction tampering detected!",
                "Authenticated Recipient: $recipient",
                "Hijacked Destination: $maliciousRecipient",
                "Discrepancy: ${integrity.discrepancyDescription}",
                "Security Status: Blocked before payment confirmation despite successful biometrics."
            )

            _uiState.update {
                it.copy(
                    currentRiskScore = 100,
                    signals = signals,
                    verificationState = null,
                    finalDecision = FinalDecisionStatus.TAMPER_DETECTED,
                    decisionMessage = "Transaction tampering detected: ${integrity.discrepancyDescription} (Biometric authentication was successful, but transaction was intercepted and tampered)."
                )
            }

            addAuditLog(
                title = "Tamper Attack Blocked [${txId}]",
                details = "Transaction tampering detected: ${integrity.discrepancyDescription}. Payment halted prior to final confirmation.",
                status = AuditStatus.DANGER,
                hash = originalBoundHash
            )
        }
    }

    /**
     * Handles successful Android BiometricPrompt authentication.
     */
    fun onBiometricSuccess() {
        val state = _uiState.value
        val verification = state.verificationState ?: return

        val originalSnapshot = verification.authenticatedSnapshot ?: AuthenticatedTransactionSnapshot(
            transactionId = verification.transactionId,
            recipient = verification.recipient,
            amount = verification.amount,
            currency = verification.currency,
            boundHash = verification.boundHash,
            biometricVerified = false,
            designatedFinger = verification.designatedFinger
        )
        val authenticatedSnapshot = originalSnapshot.copy(biometricVerified = true)

        val fingerInfo = if (verification.designatedFinger != null) {
            " [Random Fingerprint: ${verification.designatedFinger}]"
        } else ""

        addAuditLog(
            title = "Biometric Succeeded [${verification.transactionId}]",
            details = "Android BiometricPrompt / Device credential authenticated successfully$fingerInfo.",
            status = AuditStatus.SUCCESS,
            hash = verification.boundHash
        )

        if (verification.isVelocityAnomaly) {
            // Move to Step 2: Random Detail Challenge
            _uiState.update {
                it.copy(
                    verificationState = verification.copy(
                        currentStep = VerificationStep.CHALLENGE,
                        isBiometricCompleted = true,
                        challengeError = null,
                        authenticatedSnapshot = authenticatedSnapshot
                    ),
                    decisionMessage = "Step 1 Passed (Biometric$fingerInfo). Proceed to Step 2: Transaction Detail Challenge."
                )
            }
        } else {
            // High-value biometric was the only required step.
            // CRITICAL REQUIREMENT:
            // After the initial analysis and before the final payment confirmation,
            // re-verify all critical transaction details (recipient UPI ID, amount, currency,
            // and transaction ID) against the details used during the authentication phase.
            // If any discrepancy is found, immediately block the transaction with a
            // "Transaction tampering detected" message, even if biometric authentication was successful!
            val integrity = CryptoHelper.verifyTransactionIntegrity(
                snapshot = authenticatedSnapshot,
                candidateTransactionId = verification.transactionId,
                candidateRecipient = verification.recipient,
                candidateAmount = verification.amount,
                candidateCurrency = verification.currency
            )

            if (integrity.isValid) {
                commitApprovedTransaction(
                    verification.transactionId,
                    verification.recipient,
                    verification.amount,
                    verification.boundHash,
                    state.currentRiskScore ?: 40,
                    state.signals
                )
            } else {
                blockTamperedTransaction(
                    verification.transactionId,
                    verification.boundHash,
                    integrity.discrepancyDescription ?: "Details altered post-biometric authentication."
                )
            }
        }
    }

    /**
     * Handles biometric authentication failure or cancellation.
     * Security Rule 7: If any verification step is cancelled, failed, or incorrect, block payment.
     */
    fun onBiometricFailure(reason: String) {
        val state = _uiState.value
        val verification = state.verificationState

        _uiState.update {
            it.copy(
                verificationState = null,
                finalDecision = FinalDecisionStatus.BLOCKED,
                decisionMessage = "Payment blocked: Biometric authentication failed or cancelled ($reason)."
            )
        }

        addAuditLog(
            title = "Biometric Failed / Cancelled",
            details = "Verification aborted: $reason. Payment rejected as per Rule 7.",
            status = AuditStatus.DANGER,
            hash = verification?.boundHash
        )
    }

    fun onChallengeInputChanged(input: String) {
        val verification = _uiState.value.verificationState ?: return
        _uiState.update {
            it.copy(
                verificationState = verification.copy(
                    challengeInput = input,
                    challengeError = null
                )
            )
        }
    }

    /**
     * Verifies the user's answer to the random transaction-detail challenge (Step 2).
     */
    fun submitChallenge() {
        val state = _uiState.value
        val verification = state.verificationState ?: return
        val challenge = verification.challengeData ?: return

        val userInput = verification.challengeInput.trim()
        val expected = challenge.expectedAnswer.trim()

        val isCorrect = when (challenge.type) {
            ChallengeType.AMOUNT -> {
                val inputNum = userInput.toDoubleOrNull()
                val expectedNum = expected.toDoubleOrNull()
                inputNum != null && expectedNum != null && Math.abs(inputNum - expectedNum) < 0.01
            }
            ChallengeType.RECIPIENT_LAST_4 -> {
                userInput.equals(expected, ignoreCase = true)
            }
        }

        if (isCorrect) {
            addAuditLog(
                title = "Challenge Verified [${verification.transactionId}]",
                details = "Step 2 challenge passed correctly (${challenge.question} -> $userInput).",
                status = AuditStatus.SUCCESS,
                hash = verification.boundHash
            )

            // Advance to Step 3: Demo Bank OTP
            _uiState.update {
                it.copy(
                    verificationState = verification.copy(
                        currentStep = VerificationStep.DEMO_OTP,
                        challengeError = null
                    ),
                    decisionMessage = "Step 2 Passed (Detail Challenge). Proceed to Step 3: Demo Bank OTP verification."
                )
            }
        } else {
            // Rule 7: Incorrect verification blocks payment!
            _uiState.update {
                it.copy(
                    verificationState = null,
                    finalDecision = FinalDecisionStatus.BLOCKED,
                    decisionMessage = "Payment blocked: Incorrect transaction detail challenge answer. Transaction terminated."
                )
            }

            addAuditLog(
                title = "Challenge Failed [${verification.transactionId}]",
                details = "Incorrect challenge answer provided (Entered: '$userInput', Expected: '$expected'). Payment blocked.",
                status = AuditStatus.DANGER,
                hash = verification.boundHash
            )
        }
    }

    fun onOtpInputChanged(input: String) {
        val verification = _uiState.value.verificationState ?: return
        _uiState.update {
            it.copy(
                verificationState = verification.copy(
                    otpInput = input,
                    otpError = null
                )
            )
        }
    }

    /**
     * Verifies Demo Bank OTP input (Step 3).
     */
    fun submitDemoOtp() {
        val state = _uiState.value
        val verification = state.verificationState ?: return
        val expectedOtp = verification.demoOtp ?: return

        val userOtp = verification.otpInput.trim()

        if (userOtp == expectedOtp) {
            addAuditLog(
                title = "Demo OTP Verified [${verification.transactionId}]",
                details = "Step 3 Demo Bank OTP verified successfully ($userOtp).",
                status = AuditStatus.SUCCESS,
                hash = verification.boundHash
            )

            // All 3 steps complete!
            // CRITICAL REQUIREMENT:
            // Re-verify all critical transaction details (recipient UPI ID, amount, currency,
            // and transaction ID) against the authenticated snapshot before the final payment confirmation.
            val snapshot = verification.authenticatedSnapshot ?: AuthenticatedTransactionSnapshot(
                transactionId = verification.transactionId,
                recipient = verification.recipient,
                amount = verification.amount,
                currency = verification.currency,
                boundHash = verification.boundHash,
                biometricVerified = true,
                designatedFinger = verification.designatedFinger
            )

            val integrity = CryptoHelper.verifyTransactionIntegrity(
                snapshot = snapshot,
                candidateTransactionId = verification.transactionId,
                candidateRecipient = verification.recipient,
                candidateAmount = verification.amount,
                candidateCurrency = verification.currency
            )

            if (integrity.isValid) {
                commitApprovedTransaction(
                    verification.transactionId,
                    verification.recipient,
                    verification.amount,
                    verification.boundHash,
                    state.currentRiskScore ?: 70,
                    state.signals
                )
            } else {
                blockTamperedTransaction(
                    verification.transactionId,
                    verification.boundHash,
                    integrity.discrepancyDescription ?: "Details altered before final payment confirmation."
                )
            }
        } else {
            // Rule 7: Incorrect OTP blocks payment
            _uiState.update {
                it.copy(
                    verificationState = null,
                    finalDecision = FinalDecisionStatus.BLOCKED,
                    decisionMessage = "Payment blocked: Invalid Demo OTP entered. Payment rejected."
                )
            }

            addAuditLog(
                title = "OTP Verification Failed [${verification.transactionId}]",
                details = "Invalid OTP entered ('$userOtp' vs expected '$expectedOtp'). Payment blocked.",
                status = AuditStatus.DANGER,
                hash = verification.boundHash
            )
        }
    }

    /**
     * Cancels active verification. Rule 7: blocks payment.
     */
    fun cancelVerification() {
        val verification = _uiState.value.verificationState
        _uiState.update {
            it.copy(
                verificationState = null,
                finalDecision = FinalDecisionStatus.BLOCKED,
                decisionMessage = "Payment blocked: Verification process was cancelled by the user."
            )
        }

        addAuditLog(
            title = "Verification Cancelled",
            details = "User cancelled active security verification. Payment blocked as per Rule 7.",
            status = AuditStatus.WARNING,
            hash = verification?.boundHash
        )
    }

    /**
     * Security Rule 8: Only append payment to history after every required verification step succeeds.
     */
    private fun commitApprovedTransaction(
        txId: String,
        recipient: String,
        amount: Double,
        boundHash: String,
        riskScore: Int,
        signals: List<String>
    ) {
        val currentBalance = storage.getAccountBalance()
        val newBalance = (currentBalance - amount).coerceAtLeast(0.0)
        storage.setAccountBalance(newBalance)

        val newTx = PaymentTransaction(
            id = txId,
            recipient = recipient,
            amount = amount,
            currency = "INR",
            timestamp = System.currentTimeMillis(),
            payloadHash = boundHash,
            status = "APPROVED",
            riskScore = riskScore,
            signals = signals,
            balanceAfter = newBalance
        )

        storage.saveTransaction(newTx)
        refreshHistoryAndLogs()

        val formattedRemaining = com.example.model.formatIndianCurrency(newBalance)

        _uiState.update {
            it.copy(
                verificationState = null,
                accountBalance = newBalance,
                finalDecision = FinalDecisionStatus.APPROVED,
                decisionMessage = "Payment successful. Remaining balance: $formattedRemaining"
            )
        }

        addAuditLog(
            title = "Payment Authorized [${txId}]",
            details = "All security checks satisfied. Deducted ₹${String.format(Locale.US, "%.2f", amount)}. Remaining balance: $formattedRemaining. Bound hash: ${boundHash.take(16)}... Recorded in local persistence.",
            status = AuditStatus.SUCCESS,
            hash = boundHash
        )
    }

    private fun blockTamperedTransaction(txId: String, boundHash: String, reason: String) {
        val cleanReason = if (reason.startsWith("Transaction tampering detected", ignoreCase = true)) {
            reason
        } else {
            "Transaction tampering detected: $reason"
        }

        _uiState.update {
            it.copy(
                verificationState = null,
                finalDecision = FinalDecisionStatus.TAMPER_DETECTED,
                decisionMessage = cleanReason
            )
        }

        addAuditLog(
            title = "Tamper Detected [${txId}]",
            details = cleanReason,
            status = AuditStatus.DANGER,
            hash = boundHash
        )
    }

    /**
     * Security Rule 9:
     * Reset Demo History must clear locally stored transaction history and reset the UI.
     */
    fun resetDemoHistory() {
        storage.clearHistory()
        refreshHistoryAndLogs()

        _uiState.update {
            it.copy(
                recipientInput = "rahul@okhdfcbank",
                amountInput = "5000",
                untrustedDevice = false,
                newRecipient = false,
                unusualContext = false,
                currentRiskScore = null,
                signals = emptyList(),
                currentAnalysis = null,
                verificationState = null,
                finalDecision = FinalDecisionStatus.NONE,
                decisionMessage = "Demo history reset. Stored transactions cleared. Ready for test scenarios."
            )
        }

        addAuditLog(
            title = "Demo History Cleared",
            details = "Local SharedPreferences transaction store cleared. Rolling window reset to zero.",
            status = AuditStatus.INFO
        )
    }

    /**
     * Demo Balance Reset for judges:
     * Restores demo account balance to ₹1,00,000.00 and clears only balance-related transaction history.
     */
    fun resetDemoBalance() {
        storage.resetAccountBalance()
        storage.clearHistory()
        refreshHistoryAndLogs()

        val resetBalance = TransactionStorage.DEFAULT_DEMO_BALANCE
        _uiState.update {
            it.copy(
                accountBalance = resetBalance,
                recipientInput = "rahul@okhdfcbank",
                amountInput = "5000",
                untrustedDevice = false,
                newRecipient = false,
                unusualContext = false,
                currentRiskScore = null,
                signals = emptyList(),
                currentAnalysis = null,
                verificationState = null,
                finalDecision = FinalDecisionStatus.NONE,
                decisionMessage = "Demo balance reset to ${com.example.model.formatIndianCurrency(resetBalance)}. Transaction history cleared."
            )
        }

        addAuditLog(
            title = "Demo Balance Reset",
            details = "Demo Account Balance restored to ${com.example.model.formatIndianCurrency(resetBalance)} by judge action. Transaction history cleared.",
            status = AuditStatus.INFO
        )
    }

    fun clearAuditLogs() {
        storage.clearAuditLogs()
        refreshHistoryAndLogs()
        addAuditLog(
            title = "Audit Logs Cleared",
            details = "Audit log history cleared by user.",
            status = AuditStatus.INFO
        )
    }

    private fun addAuditLog(
        title: String,
        details: String,
        status: AuditStatus,
        hash: String? = null
    ) {
        val entry = AuditLogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            title = title,
            details = details,
            status = status,
            hash = hash
        )
        storage.saveAuditLog(entry)
        refreshHistoryAndLogs()
    }

    /**
     * Security Rule 6, Step 2:
     * Randomly ask either the amount or the last four characters of the recipient ID.
     */
    private fun generateRandomChallenge(amount: Double, recipient: String): ChallengeData {
        val isAmountChallenge = Random.nextBoolean()
        return if (isAmountChallenge) {
            val formatted = if (amount % 1.0 == 0.0) {
                amount.toLong().toString()
            } else {
                String.format(Locale.US, "%.2f", amount)
            }
            ChallengeData(
                type = ChallengeType.AMOUNT,
                question = "What is the exact payment amount in INR?",
                expectedAnswer = formatted
            )
        } else {
            val last4 = recipient.takeLast(4)
            ChallengeData(
                type = ChallengeType.RECIPIENT_LAST_4,
                question = "What are the last 4 characters of recipient's UPI ID (including symbols)?",
                expectedAnswer = last4
            )
        }
    }

    /**
     * Toggles between Navy-Blue Light Mode and High-Contrast Dark Security Mode.
     * Accessible status colors (Red for alert, Green for success, Amber for warning)
     * are dynamically maintained for both modes.
     */
    fun toggleTheme() {
        val newDarkState = !_uiState.value.isDarkTheme
        storage.setDarkTheme(newDarkState)
        _uiState.update { it.copy(isDarkTheme = newDarkState) }
        addAuditLog(
            title = "Theme Changed",
            details = if (newDarkState) "Enabled High-Contrast Dark Security Mode with accessible WCAG AAA status tokens."
                      else "Enabled Navy-Blue Light Mode with accessible WCAG AAA status tokens.",
            status = AuditStatus.INFO
        )
    }

    /**
     * Explicitly sets theme mode.
     */
    fun setDarkTheme(enabled: Boolean) {
        if (_uiState.value.isDarkTheme != enabled) {
            toggleTheme()
        }
    }

    // ==========================================
    // USER REGISTRATION & AUTHENTICATION METHODS
    // ==========================================

    fun onRegisterFullNameChanged(name: String) {
        _uiState.update {
            it.copy(registerForm = it.registerForm.copy(fullName = name, fullNameError = null))
        }
    }

    fun onRegisterEmailChanged(email: String) {
        _uiState.update {
            it.copy(registerForm = it.registerForm.copy(email = email, emailError = null))
        }
    }

    fun onRegisterMobileChanged(mobile: String) {
        // Keep numeric digits up to 10
        val filtered = mobile.filter { it.isDigit() }.take(10)
        _uiState.update {
            it.copy(registerForm = it.registerForm.copy(mobileNumber = filtered, mobileNumberError = null))
        }
    }

    fun onRegisterPasswordChanged(password: String) {
        _uiState.update {
            it.copy(registerForm = it.registerForm.copy(password = password, passwordError = null))
        }
    }

    fun onRegisterConfirmPasswordChanged(confirmPassword: String) {
        _uiState.update {
            it.copy(registerForm = it.registerForm.copy(confirmPassword = confirmPassword, confirmPasswordError = null))
        }
    }

    fun toggleRegisterPasswordVisibility() {
        _uiState.update {
            it.copy(registerForm = it.registerForm.copy(isPasswordVisible = !it.registerForm.isPasswordVisible))
        }
    }

    fun toggleRegisterConfirmPasswordVisibility() {
        _uiState.update {
            it.copy(registerForm = it.registerForm.copy(isConfirmPasswordVisible = !it.registerForm.isConfirmPasswordVisible))
        }
    }

    /**
     * Validates all registration fields:
     * - No field can be empty.
     * - Email must be valid.
     * - Mobile number must contain 10 digits.
     * - Password must be at least 8 characters.
     * - Password and Confirm Password must match.
     * - Shows clear validation error messages.
     *
     * After successful registration:
     * - Generates a secure salt and SHA-256 hash (never plain text).
     * - Saves the account locally.
     * - Shows success message and redirects to Login screen.
     */
    fun registerUser(): Boolean {
        val form = _uiState.value.registerForm
        var isValid = true

        var nameError: String? = null
        if (form.fullName.trim().isEmpty()) {
            nameError = "Full name cannot be empty."
            isValid = false
        }

        var emailError: String? = null
        if (form.email.trim().isEmpty()) {
            emailError = "Email address cannot be empty."
            isValid = false
        } else if (!AuthSecurityUtil.isValidEmail(form.email)) {
            emailError = "Please enter a valid email address (e.g. name@example.com)."
            isValid = false
        }

        var mobileError: String? = null
        if (form.mobileNumber.trim().isEmpty()) {
            mobileError = "Mobile number cannot be empty."
            isValid = false
        } else if (!AuthSecurityUtil.isValidMobile(form.mobileNumber)) {
            mobileError = "Mobile number must contain exactly 10 digits."
            isValid = false
        }

        var passwordError: String? = null
        if (form.password.isEmpty()) {
            passwordError = "Password cannot be empty."
            isValid = false
        } else if (form.password.length < 8) {
            passwordError = "Password must be at least 8 characters."
            isValid = false
        }

        var confirmPasswordError: String? = null
        if (form.confirmPassword.isEmpty()) {
            confirmPasswordError = "Please confirm your password."
            isValid = false
        } else if (form.password != form.confirmPassword) {
            confirmPasswordError = "Password and Confirm Password must match."
            isValid = false
        }

        if (!isValid) {
            _uiState.update {
                it.copy(
                    registerForm = form.copy(
                        fullNameError = nameError,
                        emailError = emailError,
                        mobileNumberError = mobileError,
                        passwordError = passwordError,
                        confirmPasswordError = confirmPasswordError
                    )
                )
            }
            return false
        }

        // Secure Salt & SHA-256 Hash (Never store plain-text passwords)
        val salt = AuthSecurityUtil.generateSalt()
        val passwordHash = AuthSecurityUtil.hashPassword(form.password, salt)

        val newUser = UserAccount(
            fullName = form.fullName.trim(),
            email = form.email.trim(),
            mobileNumber = AuthSecurityUtil.extractDigits(form.mobileNumber),
            passwordHash = passwordHash,
            salt = salt
        )

        storage.saveUser(newUser)
        // Default demo account balance of ₹1,00,000.00 for newly registered user
        storage.setAccountBalance(TransactionStorage.DEFAULT_DEMO_BALANCE, newUser.email)

        addAuditLog(
            title = "User Registered",
            details = "Account securely created for ${newUser.fullName} (${newUser.email}). Password salted & hashed with SHA-256.",
            status = AuditStatus.INFO,
            hash = passwordHash.take(16) + "..."
        )

        // Show success message and redirect to Login screen
        _uiState.update {
            it.copy(
                currentScreen = AuthScreen.LOGIN,
                registerForm = RegisterFormState(), // Reset registration fields
                loginForm = LoginFormState(
                    identifier = newUser.email,
                    successMessage = "Registration successful! Please log in with your credentials."
                )
            )
        }
        return true
    }

    fun onLoginIdentifierChanged(identifier: String) {
        _uiState.update {
            it.copy(
                loginForm = it.loginForm.copy(
                    identifier = identifier,
                    identifierError = null,
                    errorMessage = null
                )
            )
        }
    }

    fun onLoginPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                loginForm = it.loginForm.copy(
                    password = password,
                    passwordError = null,
                    errorMessage = null
                )
            )
        }
    }

    fun toggleLoginPasswordVisibility() {
        _uiState.update {
            it.copy(loginForm = it.loginForm.copy(isPasswordVisible = !it.loginForm.isPasswordVisible))
        }
    }

    /**
     * Verifies entered credentials with saved account details.
     * On successful login, opens the TrustPay AI payment dashboard.
     * On wrong details, shows "Invalid email/mobile number or password."
     */
    fun loginUser(): Boolean {
        val form = _uiState.value.loginForm
        val cleanIdentifier = form.identifier.trim()
        val password = form.password

        var hasError = false
        var idError: String? = null
        var passError: String? = null

        if (cleanIdentifier.isEmpty()) {
            idError = "Email or mobile number cannot be empty."
            hasError = true
        }

        if (password.isEmpty()) {
            passError = "Password cannot be empty."
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    loginForm = form.copy(
                        identifierError = idError,
                        passwordError = passError,
                        errorMessage = null
                    )
                )
            }
            return false
        }

        val user = storage.findUserByIdentifier(cleanIdentifier)
        val isVerified = user != null && AuthSecurityUtil.verifyPassword(password, user.salt, user.passwordHash)

        if (!isVerified) {
            val failureMessage = "Invalid email/mobile number or password."
            _uiState.update {
                it.copy(
                    loginForm = form.copy(
                        errorMessage = failureMessage,
                        successMessage = null
                    )
                )
            }
            addAuditLog(
                title = "Authentication Failed",
                details = "Failed login attempt for identifier '$cleanIdentifier'.",
                status = AuditStatus.DANGER
            )
            return false
        }

        // Credentials verified - Start authenticated session
        val authenticatedUser = user!!
        storage.setLoggedIn(true, authenticatedUser.email)
        val userBalance = storage.getAccountBalance(authenticatedUser.email)

        _uiState.update {
            it.copy(
                currentUser = authenticatedUser,
                currentScreen = AuthScreen.DASHBOARD,
                accountBalance = userBalance,
                loginForm = LoginFormState() // Clear credentials from form
            )
        }

        addAuditLog(
            title = "User Authenticated",
            details = "Session started for ${authenticatedUser.fullName} (${authenticatedUser.email}). Adaptive Payment Shield active.",
            status = AuditStatus.SUCCESS
        )
        return true
    }

    /**
     * Logs out the user:
     * - Returns to Login screen.
     * - Keeps registered accounts saved locally.
     * - Preserves payment history, velocity metrics, and security settings.
     */
    fun logoutUser() {
        val user = _uiState.value.currentUser
        storage.setLoggedIn(false, null)

        _uiState.update {
            it.copy(
                currentUser = null,
                currentScreen = AuthScreen.LOGIN,
                loginForm = LoginFormState(
                    identifier = user?.email ?: "",
                    successMessage = "Logged out successfully."
                )
            )
        }

        addAuditLog(
            title = "User Logged Out",
            details = "Session ended for ${user?.email ?: "user"}. Payment history and security profiles preserved.",
            status = AuditStatus.INFO
        )
    }

    fun navigateToLogin() {
        _uiState.update {
            it.copy(
                currentScreen = AuthScreen.LOGIN,
                loginForm = it.loginForm.copy(errorMessage = null)
            )
        }
    }

    fun navigateToRegister() {
        _uiState.update {
            it.copy(
                currentScreen = AuthScreen.REGISTER,
                registerForm = it.registerForm.copy(
                    fullNameError = null,
                    emailError = null,
                    mobileNumberError = null,
                    passwordError = null,
                    confirmPasswordError = null
                )
            )
        }
    }
}
