package com.example.trustpay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trustpay.ai.GeminiSecurityAnalysis
import com.example.trustpay.authentication.LoginManager
import com.example.trustpay.backend.AuthenticationApi
import com.example.trustpay.backend.SecurityApi
import com.example.trustpay.backend.TransactionApi
import com.example.trustpay.model.*
import com.example.trustpay.security.*
import com.example.trustpay.services.RiskEngineService
import com.example.trustpay.storage.TrustPayStorage
import com.example.trustpay.transactions.*
import com.example.trustpay.ui.components.*
import com.example.trustpay.ui.login_screen.LoginScreen
import com.example.trustpay.ui.otp_screen.OtpScreen
import com.example.trustpay.ui.security_dashboard.SecurityDashboard
import com.example.trustpay.ui.verification_screen.VerificationScreen
import com.example.trustpay.ui.theme.SurfaceBg
import com.example.trustpay.ui.theme.TrustPayTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrustPayTheme {
                TrustPayApp()
            }
        }
    }
}

@Composable
fun TrustPayApp() {
    var activeUser by remember { mutableStateOf(TrustPayStorage.getActiveUser()) }
    var userBalance by remember { mutableStateOf(TrustPayStorage.getBalance()) }
    var transactions by remember { mutableStateOf(TrustPayStorage.getTransactions()) }
    var auditLogs by remember { mutableStateOf(TrustPayStorage.getAuditLogs()) }
    var dualAuthList by remember { mutableStateOf(TrustPayStorage.getDualAuthTransactions()) }

    // Payment Form state
    var recipientInput by remember { mutableStateOf("merchant.pay@okhdfcbank") }
    var amountInput by remember { mutableStateOf("2500") }
    var riskFactors by remember { mutableStateOf(RiskEngineService.RiskFactors()) }

    // Modals state
    var showResetBalanceDialog by remember { mutableStateOf(false) }
    var showReceiveQrModal by remember { mutableStateOf(false) }
    var showScanPayModal by remember { mutableStateOf(false) }
    var showDualAuthModal by remember { mutableStateOf(false) }
    var showDistanceClassifier by remember { mutableStateOf(false) }
    var showAdminSocModal by remember { mutableStateOf(false) }
    var showDemoScenarioModal by remember { mutableStateOf(false) }
    var showSecurityDashboard by remember { mutableStateOf(false) }
    var showOtpModal by remember { mutableStateOf(false) }

    // Three-Step Verification Pipeline State
    var activeVerificationSession by remember { mutableStateOf<TransactionVerification.VerificationSession?>(null) }
    var lastAiAssessment by remember { mutableStateOf<GeminiSecurityAnalysis.AiSecurityResponse?>(null) }
    var lastTransactionRecord by remember { mutableStateOf<PaymentTransactionRecord?>(null) }

    // Verification Dialog state
    var verificationState by remember { mutableStateOf<VerificationState?>(null) }
    var activeSnapshot by remember { mutableStateOf<AuthenticatedTransactionSnapshot?>(null) }

    fun refreshState() {
        activeUser = TrustPayStorage.getActiveUser()
        userBalance = TrustPayStorage.getBalance()
        transactions = TrustPayStorage.getTransactions()
        auditLogs = TrustPayStorage.getAuditLogs()
        dualAuthList = TrustPayStorage.getDualAuthTransactions()
    }

    val currentRiskResult = remember(amountInput, recipientInput, riskFactors, transactions) {
        val parsedAmt = amountInput.toDoubleOrNull() ?: 0.0
        val txId = "TXN_PREVIEW"
        RiskEngineService.evaluateTransactionRisk(txId, recipientInput, parsedAmt, riskFactors)
    }

    val rollingTxns = remember(transactions) {
        TrustPayStorage.getRollingWindowTransactions(5)
    }
    val rollingTotal = remember(transactions) {
        TrustPayStorage.getRollingWindowTotalAmount(5)
    }
    val isVelocityBreached = remember(rollingTxns, rollingTotal) {
        (rollingTxns.size >= 3) || (rollingTotal >= 50000.0)
    }

    fun executeTransferFinal(txId: String, recipient: String, amount: Double, riskScore: Int, bindingHash: String) {
        if (userBalance < amount) {
            TrustPayStorage.addAuditLog(
                "Transfer Failed",
                "Insufficient funds for transfer of ${CryptoUtils.formatIndianCurrency(amount)} to $recipient.",
                AuditStatus.DANGER
            )
            refreshState()
            return
        }

        val newBal = userBalance - amount
        TrustPayStorage.setBalance(newBal)

        val tx = PaymentTransaction(
            id = txId,
            recipient = recipient,
            amount = amount,
            status = "APPROVED",
            bindingHash = bindingHash,
            riskScore = riskScore,
            balanceAfter = newBal,
            senderId = activeUser?.email ?: "default"
        )
        TrustPayStorage.addTransaction(tx)

        TrustPayStorage.addAuditLog(
            "Payment Released & Sealed",
            "Transferred ${CryptoUtils.formatIndianCurrency(amount)} to $recipient.",
            AuditStatus.SUCCESS,
            bindingHash
        )
        refreshState()
    }

    fun initiatePaymentFlow() {
        val amt = amountInput.toDoubleOrNull() ?: 0.0
        if (amt <= 0) return

        val userEmail = activeUser?.email ?: "alex.pay@trustpay.demo"
        val creation = TransactionCreation.createTransaction(
            TransactionCreation.CreationRequest(
                senderEmail = userEmail,
                recipientVpa = recipientInput,
                amount = amt
            )
        )

        if (!creation.isValid) {
            TrustPayStorage.addAuditLog(
                "Transaction Blocked",
                creation.errorMessage ?: "Invalid transaction parameters",
                AuditStatus.DANGER
            )
            refreshState()
            return
        }

        // Initialize 3-Step Verification Session
        val session = TransactionVerification.startSession(
            transactionId = creation.transactionId,
            senderEmail = userEmail,
            recipient = recipientInput,
            amount = amt,
            bindingHash = creation.bindingHash
        )

        // Evaluate Step 1: Device Recognition
        val registeredDev = activeUser?.registeredDeviceId
        val devResult = DeviceRecognition.evaluateDevice(registeredDev)
        session.deviceResult = devResult

        // Evaluate location anomaly if enabled in risk factors
        if (riskFactors.unusualContext) {
            session.locationResult = LocationSecurity.evaluateTransactionLocation(
                userHomeLat = 19.0760,
                userHomeLon = 72.8777,
                txLat = 28.7041,
                txLon = 77.1025,
                maxAllowedKm = 100.0
            )
        }

        activeVerificationSession = session
    }

    if (activeUser == null) {
        LoginRegisterScreen(
            onLoginSuccess = {
                refreshState()
            }
        )
    } else {
        Scaffold(
            topBar = {
                HeaderBanner(
                    activeUser = activeUser,
                    pendingDualAuthCount = dualAuthList.count { it.status == DualAuthStatus.PENDING_SECOND_AUTH } +
                            TrustPayStorage.getSecondSignatureTransactions().count {
                                it.status == com.example.trustpay.model.SecondSignatureStatus.SECOND_SIGNATURE_REQUIRED ||
                                        it.status == com.example.trustpay.model.SecondSignatureStatus.PENDING
                            },
                    onSwitchUser = {
                        val users = TrustPayStorage.getUsers()
                        val other = users.find { it.email != activeUser?.email } ?: users.first()
                        TrustPayStorage.setActiveUser(other)
                        refreshState()
                    },
                    onLogout = {
                        TrustPayStorage.setActiveUser(null)
                        refreshState()
                    },
                    onOpenScan = { showScanPayModal = true },
                    onOpenReceive = { showReceiveQrModal = true },
                    onOpenDualAuth = { showDualAuthModal = true },
                    onOpenDistance = { showDistanceClassifier = true },
                    onOpenSoc = { showAdminSocModal = true },
                    onOpenScenarios = { showDemoScenarioModal = true },
                    onOpenDashboard = { showSecurityDashboard = true }
                )
            },
            containerColor = SurfaceBg
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Account Balance Card
                AccountBalanceCard(
                    balance = userBalance,
                    onResetBalanceClick = { showResetBalanceDialog = true }
                )

                // Payment Input Card
                PaymentInputCard(
                    recipient = recipientInput,
                    onRecipientChange = { recipientInput = it },
                    amountString = amountInput,
                    onAmountChange = { amountInput = it },
                    riskFactors = riskFactors,
                    onRiskFactorsChange = { riskFactors = it },
                    onInitiatePayment = { initiatePaymentFlow() }
                )

                // Real-Time Risk Assessment Card
                RiskAssessmentCard(
                    riskResult = currentRiskResult
                )

                // Rolling 5m Velocity Anomaly Monitor
                VelocityMonitorCard(
                    txnCount = rollingTxns.size,
                    cumulativeAmount = rollingTotal,
                    isBreached = isVelocityBreached
                )

                // Recent Transactions Ledger
                TransactionHistoryCard(
                    transactions = transactions
                )

                // Security Audit Logs Section
                AuditLogSection(
                    logs = auditLogs
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Modals & Dialogs
        if (showResetBalanceDialog) {
            ResetBalanceDialog(
                currentBalance = userBalance,
                onConfirm = { newBal ->
                    TrustPayStorage.resetBalance(newBal)
                    showResetBalanceDialog = false
                    refreshState()
                },
                onDismiss = { showResetBalanceDialog = false }
            )
        }

        if (showReceiveQrModal) {
            ReceiveQrModal(
                onDismiss = { showReceiveQrModal = false; refreshState() }
            )
        }

        if (showScanPayModal) {
            ScanPayModal(
                onPaymentSuccess = { tx ->
                    refreshState()
                },
                onDismiss = { showScanPayModal = false; refreshState() }
            )
        }

        if (showDualAuthModal) {
            DualAuthModal(
                onDismiss = { showDualAuthModal = false; refreshState() }
            )
        }

        if (showDistanceClassifier) {
            DistanceClassifierView(
                onDismiss = { showDistanceClassifier = false }
            )
        }

        if (showAdminSocModal) {
            AdminSocModal(
                onDismiss = { showAdminSocModal = false }
            )
        }

        if (showDemoScenarioModal) {
            DemoScenarioModal(
                onSelectScenario = { rec, amt, factors ->
                    recipientInput = rec
                    amountInput = amt
                    riskFactors = factors
                },
                onDismiss = { showDemoScenarioModal = false }
            )
        }

        // Three-Step Verification Dialog (Step 1 Device -> Step 2 Biometric -> Step 3 Face ID)
        activeVerificationSession?.let { session ->
            if (session.currentPhase != VerificationPhase.COMPLETED && !showOtpModal) {
                VerificationScreen(
                    session = session,
                    onStep1DeviceComplete = {
                        session.currentPhase = VerificationPhase.STEP_2_BIOMETRIC
                    },
                    onStep2BiometricComplete = {
                        session.biometricVerified = true
                        session.currentPhase = VerificationPhase.STEP_3_FACE
                    },
                    onStep3FaceComplete = {
                        session.faceResult = FaceVerification.FaceVerificationResult(
                            faceVerified = true,
                            authenticationMethod = "APPROVED_SECURITY_PIN",
                            isHardwareFaceAvailable = false,
                            riskLevel = "LOW",
                            statusMessage = "Face/PIN authorization verified"
                        )
                    },
                    onProceedToOtp = {
                        showOtpModal = true
                    },
                    onRegisterCurrentDevice = { devId, devName ->
                        AuthenticationApi.registerDeviceToUser(
                            userEmail = session.senderEmail,
                            newDeviceId = devId,
                            newDeviceName = devName
                        )
                        TrustPayStorage.addAuditLog(
                            "Device Registered",
                            "Device $devName ($devId) bound to user profile.",
                            AuditStatus.SUCCESS
                        )
                        refreshState()
                    },
                    onCancel = {
                        activeVerificationSession = null
                    }
                )
            }
        }

        // Demo Bank OTP Dialog
        if (showOtpModal && activeVerificationSession != null) {
            val session = activeVerificationSession!!
            OtpScreen(
                onOtpVerified = {
                    session.otpVerified = true
                    showOtpModal = false

                    // AI Transaction Risk Analysis & Structured Assessment
                    val ai = TransactionVerification.performAiAnalysis(session)
                    lastAiAssessment = ai

                    // Deterministic Backend Authorization
                    val outcome = TransactionVerification.completeAuthorization(session)
                    when (outcome) {
                        is TransactionAuthorization.AuthorizationOutcome.Approved -> {
                            executeTransferFinal(
                                session.transactionId,
                                session.recipient,
                                session.amount,
                                ai.riskScore,
                                session.bindingHash
                            )
                            lastTransactionRecord = outcome.transaction
                        }
                        is TransactionAuthorization.AuthorizationOutcome.Blocked -> {
                            TrustPayStorage.addAuditLog(
                                "TRANSACTION BLOCKED",
                                outcome.reason,
                                AuditStatus.DANGER,
                                session.bindingHash
                            )
                            lastTransactionRecord = PaymentTransactionRecord(
                                transactionId = session.transactionId,
                                senderEmail = session.senderEmail,
                                recipient = session.recipient,
                                amount = session.amount,
                                status = "BLOCKED",
                                riskLevel = ai.riskLevel,
                                riskScore = ai.riskScore,
                                deviceVerified = session.deviceResult?.deviceVerified == true,
                                biometricVerified = session.biometricVerified,
                                faceVerified = session.faceResult?.faceVerified == true,
                                otpVerified = session.otpVerified,
                                bindingHash = session.bindingHash,
                                recommendedAction = "BLOCK",
                                notes = outcome.reason
                            )
                            refreshState()
                        }
                        is TransactionAuthorization.AuthorizationOutcome.RequiresExtraVerification -> {
                            TrustPayStorage.addAuditLog(
                                "EXTRA VERIFICATION MANDATED",
                                outcome.reason,
                                AuditStatus.WARNING,
                                session.bindingHash
                            )
                        }
                    }

                    // Automatically reveal the Security Dashboard with full transparency
                    showSecurityDashboard = true
                },
                onCancel = {
                    showOtpModal = false
                    activeVerificationSession = null
                }
            )
        }

        // Central Security Dashboard Modal
        if (showSecurityDashboard) {
            SecurityDashboard(
                lastTransaction = lastTransactionRecord,
                aiAssessment = lastAiAssessment,
                onDismiss = { showSecurityDashboard = false }
            )
        }

        // Verification Dialog
        verificationState?.let { state ->
            VerificationDialog(
                state = state,
                onBiometricSuccess = {
                    val snapshot = activeSnapshot
                    if (snapshot != null) {
                        val valid = CryptoUtils.verifyTransactionIntegrity(
                            snapshot, state.recipient, state.amount
                        )
                        if (valid) {
                            executeTransferFinal(
                                state.transactionId, state.recipient, state.amount,
                                currentRiskResult.score, snapshot.bindingHash
                            )
                        } else {
                            TrustPayStorage.addAuditLog(
                                "CRITICAL: Tamper Attack Blocked",
                                "Binding digest mismatch! Transaction payload was altered.",
                                AuditStatus.DANGER,
                                snapshot.bindingHash
                            )
                        }
                    }
                    verificationState = null
                    refreshState()
                },
                onChallengeAnswer = { ans ->
                    val expected = state.challengeData?.expectedAnswer ?: ""
                    if (ans.trim().equals(expected.trim(), ignoreCase = true)) {
                        verificationState = state.copy(currentStep = VerificationStep.BIOMETRIC)
                    }
                },
                onOtpSubmit = { enteredOtp ->
                    if (enteredOtp.trim() == state.demoOtp.trim()) {
                        verificationState = state.copy(currentStep = VerificationStep.BIOMETRIC)
                    }
                },
                onSimulateTamper = {
                    // Tamper Simulation: modifies amount behind the scenes
                    val snapshot = activeSnapshot
                    if (snapshot != null) {
                        val tamperedAmount = state.amount + 50000.0
                        val isTampered = !CryptoUtils.verifyTransactionIntegrity(
                            snapshot, state.recipient, tamperedAmount
                        )
                        if (isTampered) {
                            TrustPayStorage.addTransaction(
                                PaymentTransaction(
                                    id = state.transactionId,
                                    recipient = state.recipient,
                                    amount = tamperedAmount,
                                    status = "TAMPER_DETECTED",
                                    bindingHash = snapshot.bindingHash,
                                    riskScore = 100,
                                    senderId = activeUser?.email ?: "default",
                                    note = "Tamper Detected: Payload altered in-flight!"
                                )
                            )
                            TrustPayStorage.addAuditLog(
                                "TAMPER ATTACK INTERCEPTED",
                                "In-flight payload modification detected! Snapshot: ${CryptoUtils.formatIndianCurrency(state.amount)} vs Tampered: ${CryptoUtils.formatIndianCurrency(tamperedAmount)}. Transaction blocked.",
                                AuditStatus.DANGER,
                                snapshot.bindingHash
                            )
                        }
                    }
                    verificationState = null
                    refreshState()
                },
                onDismiss = {
                    verificationState = null
                }
            )
        }
    }
}
