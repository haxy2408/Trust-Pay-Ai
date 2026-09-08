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
import com.example.trustpay.model.*
import com.example.trustpay.security.CryptoUtils
import com.example.trustpay.services.RiskEngineService
import com.example.trustpay.storage.TrustPayStorage
import com.example.trustpay.ui.components.*
import com.example.trustpay.ui.theme.Navy900
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

        val txId = CryptoUtils.generateTransactionId()
        val snapshot = CryptoUtils.createSnapshot(txId, recipientInput, amt)
        activeSnapshot = snapshot

        val risk = RiskEngineService.evaluateTransactionRisk(txId, recipientInput, amt, riskFactors)

        if (risk.score < 35 && !risk.isVelocityAnomaly) {
            // Nominal Low-Risk -> Instant 1-click release
            executeTransferFinal(txId, recipientInput, amt, risk.score, snapshot.bindingHash)
        } else {
            // Requires verification step
            val nextStep = if (risk.requiresOtp) {
                VerificationStep.DEMO_OTP
            } else {
                VerificationStep.BIOMETRIC
            }

            verificationState = VerificationState(
                transactionId = txId,
                recipient = recipientInput,
                amount = amt,
                currentStep = nextStep,
                isVelocityAnomaly = risk.isVelocityAnomaly,
                designatedFinger = "Right Index Finger",
                challengeData = ChallengeData(
                    question = "Confirm beneficiary domain for $recipientInput:",
                    expectedAnswer = recipientInput.substringAfter("@")
                ),
                demoOtp = CryptoUtils.generateDemoOtp()
            )
        }
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
                    pendingDualAuthCount = dualAuthList.count { it.status == DualAuthStatus.PENDING_SECOND_AUTH },
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
                    onOpenScenarios = { showDemoScenarioModal = true }
                )
            },
            containerColor = Navy900
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
