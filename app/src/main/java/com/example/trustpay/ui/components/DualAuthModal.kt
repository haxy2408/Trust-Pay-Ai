package com.example.trustpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.trustpay.backend.SecondSignatureApi
import com.example.trustpay.model.*
import com.example.trustpay.security.CryptoUtils
import com.example.trustpay.security.SecondSignatureEngine
import com.example.trustpay.storage.TrustPayStorage
import com.example.trustpay.ui.theme.*
import java.util.Locale
import java.util.UUID

@Composable
fun DualAuthModal(
    onDismiss: () -> Unit
) {
    var activeUser by remember { mutableStateOf(TrustPayStorage.getActiveUser()) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Review & Sign, 1: Initiate, 2: Security Lab, 3: Directory
    var secondSigList by remember { mutableStateOf(TrustPayStorage.getSecondSignatureTransactions()) }
    val coSigners = remember { TrustPayStorage.getCoSigners() }

    // Initiate form state
    var recipientInput by remember { mutableStateOf("aws.corporate@icicibank") }
    var amountInput by remember { mutableStateOf("85000") }
    var purposeInput by remember { mutableStateOf("Multi-Region Cloud Infrastructure Expansion") }
    var selectedTxType by remember { mutableStateOf("CORPORATE_PAYMENT") }
    var selectedCoSigner by remember { mutableStateOf(coSigners.firstOrNull()) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }
    var isFeedbackError by remember { mutableStateOf(false) }

    fun refreshState() {
        secondSigList = TrustPayStorage.getSecondSignatureTransactions()
        activeUser = TrustPayStorage.getActiveUser()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(2.dp)
                .testTag("dual_auth_modal")
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TrustBlueContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Second Signature",
                                tint = TrustBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "The Second Signature",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalText
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SecurityGreenBg
                                ) {
                                    Text(
                                        text = "2-PERSON INTEGRITY",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecurityGreen,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Cryptographic Dual-Party Independent Authorization",
                                fontSize = 11.sp,
                                color = SlateText
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateText)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Identity Switcher Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SecurityGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active Signer: ",
                                fontSize = 11.sp,
                                color = SlateText
                            )
                            Text(
                                text = "${activeUser?.fullName} (${activeUser?.email})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )
                        }

                        TextButton(
                            onClick = {
                                val users = TrustPayStorage.getUsers()
                                val other = users.find { it.email != activeUser?.email } ?: users.first()
                                TrustPayStorage.setActiveUser(other)
                                refreshState()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp), tint = TrustBlue)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Switch User", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TrustBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceVariant,
                    contentColor = TrustBlue,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    val pendingCount = secondSigList.count {
                        it.status == SecondSignatureStatus.SECOND_SIGNATURE_REQUIRED ||
                                it.status == SecondSignatureStatus.PENDING
                    }
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; refreshState() },
                        text = {
                            Text(
                                text = if (pendingCount > 0) "Review & Sign ($pendingCount)" else "Review & Sign",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) TrustBlue else SlateText
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Initiate",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) TrustBlue else SlateText
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2; refreshState() },
                        text = {
                            Text(
                                text = "Security Lab",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 2) TrustBlue else SlateText
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = {
                            Text(
                                text = "Policy & Roles",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 3) TrustBlue else SlateText
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Feedback Alert if present
                if (actionFeedback != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isFeedbackError) SecurityRedBg else SecurityGreenBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFeedbackError) SecurityRed else SecurityGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isFeedbackError) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isFeedbackError) SecurityRed else SecurityGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = actionFeedback!!,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isFeedbackError) SecurityRed else SecurityGreen
                            )
                        }
                    }
                }

                // Body content based on tab
                when (selectedTab) {
                    0 -> ReviewAndSignTab(
                        transactions = secondSigList,
                        activeUser = activeUser,
                        onApprove = { tx ->
                            val res = SecondSignatureApi.submitSecondSignature(
                                transactionId = tx.id,
                                signerEmail = activeUser?.email ?: "cosigner@trustpay.demo",
                                signerName = activeUser?.fullName ?: "CoSigner"
                            )
                            actionFeedback = res.message
                            isFeedbackError = !res.success
                            refreshState()
                        },
                        onReject = { tx, reason ->
                            val ok = SecondSignatureApi.rejectTransaction(
                                transactionId = tx.id,
                                signerEmail = activeUser?.email ?: "cosigner@trustpay.demo",
                                reason = reason
                            )
                            actionFeedback = if (ok) "Transaction declined." else "Rejection failed."
                            isFeedbackError = true
                            refreshState()
                        },
                        onSwitchToCoSigner = { designatedEmail ->
                            val users = TrustPayStorage.getUsers()
                            val target = users.find { it.email.equals(designatedEmail, ignoreCase = true) }
                            if (target != null) {
                                TrustPayStorage.setActiveUser(target)
                            } else {
                                val other = users.find { it.email != activeUser?.email } ?: users.first()
                                TrustPayStorage.setActiveUser(other)
                            }
                            refreshState()
                        }
                    )
                    1 -> InitiateTransferTab(
                        recipientInput = recipientInput,
                        onRecipientChange = { recipientInput = it },
                        amountInput = amountInput,
                        onAmountChange = { amountInput = it },
                        purposeInput = purposeInput,
                        onPurposeChange = { purposeInput = it },
                        selectedTxType = selectedTxType,
                        onTxTypeChange = { selectedTxType = it },
                        selectedCoSigner = selectedCoSigner,
                        onCoSignerChange = { selectedCoSigner = it },
                        coSigners = coSigners,
                        onInitiate = {
                            val amt = amountInput.toDoubleOrNull() ?: 50000.0
                            val signer = selectedCoSigner ?: coSigners.first()
                            val res = SecondSignatureApi.createTransaction(
                                senderEmail = activeUser?.email ?: "rahul@example.com",
                                senderName = activeUser?.fullName ?: "Rahul Sharma",
                                recipientId = recipientInput.trim(),
                                amount = amt,
                                transactionType = selectedTxType,
                                purposeDescription = purposeInput.trim(),
                                designatedSecondSignerId = signer.email,
                                designatedSecondSignerName = signer.fullName,
                                designatedSecondSignerRole = signer.role
                            )
                            if (res.success) {
                                actionFeedback = "Transaction initiated! Sealed with 1st signature. Awaiting 2nd signature."
                                isFeedbackError = false
                                selectedTab = 0
                                refreshState()
                            } else {
                                actionFeedback = res.message
                                isFeedbackError = true
                            }
                        }
                    )
                    2 -> SecurityLabTab(
                        transactions = secondSigList,
                        activeUser = activeUser,
                        onRunTamperAmount = { tx ->
                            val res = SecondSignatureApi.submitSecondSignature(
                                transactionId = tx.id,
                                signerEmail = "priya@example.com",
                                signerName = "Priya Patel",
                                simulatedTamperedAmount = tx.amount + 50000.0
                            )
                            actionFeedback = res.message
                            isFeedbackError = !res.success
                            refreshState()
                        },
                        onRunTamperRecipient = { tx ->
                            val res = SecondSignatureApi.submitSecondSignature(
                                transactionId = tx.id,
                                signerEmail = "priya@example.com",
                                signerName = "Priya Patel",
                                simulatedTamperedRecipient = "hacker.beneficiary@maliciousbank"
                            )
                            actionFeedback = res.message
                            isFeedbackError = !res.success
                            refreshState()
                        },
                        onRunSelfApprovalTest = { tx ->
                            val res = SecondSignatureApi.submitSecondSignature(
                                transactionId = tx.id,
                                signerEmail = tx.senderId, // Intentional violation: same user!
                                signerName = tx.senderName
                            )
                            actionFeedback = res.message
                            isFeedbackError = !res.success
                            refreshState()
                        },
                        onRunReplayTest = { tx ->
                            // Attempt to approve already executed or reused nonce
                            val res = SecondSignatureApi.submitSecondSignature(
                                transactionId = tx.id,
                                signerEmail = "priya@example.com",
                                signerName = "Priya Patel"
                            )
                            actionFeedback = res.message
                            isFeedbackError = !res.success
                            refreshState()
                        },
                        onResetDemo = {
                            val seedTxId = "TXN_2ND_CORP88"
                            val seedNonce = "NONCE_INIT_${System.currentTimeMillis() % 10000}"
                            val seedCreatedAt = System.currentTimeMillis()
                            val seedFingerprint = SecondSignatureEngine.computeTransactionFingerprint(
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
                            val seedFirstSig = SecondSignatureEngine.generateSignerSignature(
                                signerId = "rahul@example.com",
                                signerName = "Rahul Sharma",
                                signerRole = "Initiator / Primary Signer",
                                transactionId = seedTxId,
                                transactionFingerprint = seedFingerprint,
                                transactionVersion = 1,
                                timestamp = seedCreatedAt,
                                nonce = seedNonce
                            )
                            val freshTx = SecondSignatureTransaction(
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
                                expiresAt = seedCreatedAt + SecondSignatureEngine.SECOND_SIGNATURE_EXPIRY_MS,
                                designatedSecondSignerId = "priya@example.com",
                                designatedSecondSignerName = "Priya Patel",
                                designatedSecondSignerRole = "Chief Financial Officer (CFO)",
                                firstSignature = seedFirstSig,
                                riskScore = 65,
                                nonce = seedNonce
                            )
                            TrustPayStorage.updateSecondSignatureTransaction(freshTx)
                            actionFeedback = "Demo scenario reset with clean pending corporate transaction."
                            isFeedbackError = false
                            refreshState()
                        }
                    )
                    3 -> DirectoryAndPolicyTab(coSigners = coSigners)
                }
            }
        }
    }
}

@Composable
private fun ReviewAndSignTab(
    transactions: List<SecondSignatureTransaction>,
    activeUser: UserAccount?,
    onApprove: (SecondSignatureTransaction) -> Unit,
    onReject: (SecondSignatureTransaction, String) -> Unit,
    onSwitchToCoSigner: (String) -> Unit
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No pending second signatures.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                Text("Initiate a high-risk transfer in the Initiate tab.", fontSize = 12.sp, color = SlateText)
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(transactions) { tx ->
                val isPending = tx.status == SecondSignatureStatus.SECOND_SIGNATURE_REQUIRED ||
                        tx.status == SecondSignatureStatus.PENDING
                val isExecuted = tx.status == SecondSignatureStatus.EXECUTED
                val isTampered = tx.status == SecondSignatureStatus.TAMPER_DETECTED
                val isRejected = tx.status == SecondSignatureStatus.REJECTED
                val isExpired = tx.status == SecondSignatureStatus.EXPIRED

                val isSelf = activeUser?.email.equals(tx.senderId, ignoreCase = true)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = when {
                            isTampered -> SecurityRed
                            isExecuted -> SecurityGreen
                            isPending -> SecurityAmber
                            else -> CardBorder
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Top Header Status Banner
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isTampered -> SecurityRedBg
                                isExecuted -> SecurityGreenBg
                                isPending -> SecurityAmberBg
                                else -> SurfaceVariant
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when {
                                            isTampered -> Icons.Default.GppBad
                                            isExecuted -> Icons.Default.CheckCircle
                                            isPending -> Icons.Default.Lock
                                            else -> Icons.Default.Info
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            isTampered -> SecurityRed
                                            isExecuted -> SecurityGreen
                                            isPending -> SecurityAmber
                                            else -> SlateText
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            isTampered -> "🚨 TAMPER DETECTED — BLOCKED"
                                            isExecuted -> "✓ DUAL SIGNATURE AUTHORIZED & EXECUTED"
                                            isRejected -> "❌ TRANSACTION REJECTED"
                                            isExpired -> "⏰ APPROVAL WINDOW EXPIRED"
                                            else -> "🔐 SECOND SIGNATURE REQUIRED"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isTampered -> SecurityRed
                                            isExecuted -> SecurityGreen
                                            isPending -> SecurityAmber
                                            else -> SlateText
                                        }
                                    )
                                }

                                if (isPending) {
                                    Text(
                                        text = "${tx.getRemainingSeconds()}s left",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecurityAmber
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Amount and Recipient
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = CryptoUtils.formatIndianCurrency(tx.amount),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalText
                                )
                                Text(
                                    text = "To: ${tx.recipientId}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CharcoalText
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TrustBlueContainer
                            ) {
                                Text(
                                    text = tx.transactionType.replace("_", " "),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TrustBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Purpose: ${tx.purposeDescription}",
                            fontSize = 11.sp,
                            color = SlateText
                        )
                        Text(
                            text = "Txn ID: ${tx.id} (v${tx.transactionVersion})",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = SlateMuted
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Visual Authorization Timeline
                        TimelineBar(status = tx.status)

                        Spacer(modifier = Modifier.height(10.dp))

                        // Signer Status Checklist
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                // First Signer Status
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SecurityGreen,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "First Authorization Completed",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CharcoalText
                                        )
                                        Text(
                                            text = "Signed by ${tx.senderName} (${tx.senderId})",
                                            fontSize = 10.sp,
                                            color = SlateText
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Second Signer Status
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isExecuted) Icons.Default.CheckCircle else if (isPending) Icons.Default.RadioButtonUnchecked else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (isExecuted) SecurityGreen else if (isPending) SecurityAmber else SecurityRed,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = if (isExecuted) "Second Authorization Completed" else "Waiting for Second Authorization",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isExecuted) CharcoalText else if (isPending) SecurityAmber else SecurityRed
                                        )
                                        Text(
                                            text = "Designated Approver: ${tx.designatedSecondSignerName} (${tx.designatedSecondSignerRole})",
                                            fontSize = 10.sp,
                                            color = SlateText
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ℹ️ Two independent authorizations are required to complete this transaction.",
                            fontSize = 10.sp,
                            color = SlateText
                        )

                        // Action Buttons / Warning
                        if (isPending) {
                            Spacer(modifier = Modifier.height(10.dp))
                            if (isSelf) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SecurityAmberBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SecurityAmber.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "⚠️ Same-User Policy Guard: Initiator (${tx.senderName}) cannot provide both signatures.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SecurityAmber
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { onSwitchToCoSigner(tx.designatedSecondSignerId) },
                                                colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(36.dp)
                                            ) {
                                                Text("Switch to ${tx.designatedSecondSignerName}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WhitePure)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onApprove(tx) },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = WhitePure)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("APPROVE & SIGN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WhitePure)
                                    }

                                    OutlinedButton(
                                        onClick = { onReject(tx, "Declined by second signer") },
                                        modifier = Modifier.weight(0.6f).height(40.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecurityRed),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SecurityRed),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("REJECT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineBar(status: SecondSignatureStatus) {
    val stepIndex = when (status) {
        SecondSignatureStatus.PENDING -> 1
        SecondSignatureStatus.FIRST_SIGNATURE_COMPLETED -> 2
        SecondSignatureStatus.SECOND_SIGNATURE_REQUIRED -> 3
        SecondSignatureStatus.VERIFIED -> 4
        SecondSignatureStatus.AUTHORIZED -> 5
        SecondSignatureStatus.EXECUTED -> 6
        SecondSignatureStatus.REJECTED, SecondSignatureStatus.EXPIRED, SecondSignatureStatus.FAILED, SecondSignatureStatus.TAMPER_DETECTED -> 0
    }

    val steps = listOf("Created", "1st Sig", "2nd Req", "Verify", "Auth", "Executed")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, name ->
            val isPassed = (index + 1) < stepIndex
            val isCurrent = (index + 1) == stepIndex
            val isFailed = status == SecondSignatureStatus.TAMPER_DETECTED || status == SecondSignatureStatus.REJECTED || status == SecondSignatureStatus.EXPIRED

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isFailed && isCurrent -> SecurityRed
                                isPassed -> SecurityGreen
                                isCurrent -> SecurityAmber
                                else -> SlateMuted.copy(alpha = 0.3f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPassed) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = WhitePure, modifier = Modifier.size(10.dp))
                    } else if (isCurrent) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(WhitePure))
                    }
                }
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = name,
                    fontSize = 9.sp,
                    fontWeight = if (isCurrent || isPassed) FontWeight.Bold else FontWeight.Normal,
                    color = if (isPassed) SecurityGreen else if (isCurrent) CharcoalText else SlateMuted
                )
                if (index < steps.size - 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(2.dp)
                            .background(if (isPassed) SecurityGreen else SlateMuted.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }
    }
}

@Composable
private fun InitiateTransferTab(
    recipientInput: String,
    onRecipientChange: (String) -> Unit,
    amountInput: String,
    onAmountChange: (String) -> Unit,
    purposeInput: String,
    onPurposeChange: (String) -> Unit,
    selectedTxType: String,
    onTxTypeChange: (String) -> Unit,
    selectedCoSigner: CoSigner?,
    onCoSignerChange: (CoSigner) -> Unit,
    coSigners: List<CoSigner>,
    onInitiate: () -> Unit
) {
    val txTypes = listOf(
        "CORPORATE_PAYMENT" to "Corporate Payment",
        "HIGH_VALUE_PAYMENT" to "High-Value Transfer",
        "JOINT_ACCOUNT" to "Joint Account",
        "GUARDIAN_APPROVAL" to "Guardian Approval",
        "RISK_TRIGGERED" to "Risk Triggered"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Select Transaction Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            txTypes.forEach { (typeKey, typeLabel) ->
                val isSelected = selectedTxType == typeKey
                Surface(
                    onClick = { onTxTypeChange(typeKey) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) TrustBlue else SurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) TrustBlue else CardBorder)
                ) {
                    Text(
                        text = typeLabel,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) WhitePure else CharcoalText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = recipientInput,
            onValueChange = onRecipientChange,
            label = { Text("Corporate / Beneficiary VPA") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrustBlue,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = CharcoalText,
                unfocusedTextColor = CharcoalText
            ),
            shape = RoundedCornerShape(8.dp)
        )

        OutlinedTextField(
            value = amountInput,
            onValueChange = onAmountChange,
            label = { Text("Transfer Amount (₹ INR)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrustBlue,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = CharcoalText,
                unfocusedTextColor = CharcoalText
            ),
            shape = RoundedCornerShape(8.dp)
        )

        OutlinedTextField(
            value = purposeInput,
            onValueChange = onPurposeChange,
            label = { Text("Purpose / Authorization Note") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrustBlue,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = CharcoalText,
                unfocusedTextColor = CharcoalText
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Text("Select Authorized Second Signer:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalText)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            coSigners.forEach { signer ->
                val isSelected = selectedCoSigner?.id == signer.id
                Surface(
                    onClick = { onCoSignerChange(signer) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) TrustBlueContainer else SurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) TrustBlue else CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(signer.fullName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                            Text(signer.role, fontSize = 10.sp, color = SlateText)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = TrustBlueContainer.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "The server will reconstruct a SHA-256 fingerprint, seal it with your First Signature, and lock funds until the second signature is provided.",
                    fontSize = 10.sp,
                    color = CharcoalText,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = onInitiate,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TrustBlue)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = WhitePure, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Initiate & Sign (1st Signature)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WhitePure)
        }
    }
}

@Composable
private fun SecurityLabTab(
    transactions: List<SecondSignatureTransaction>,
    activeUser: UserAccount?,
    onRunTamperAmount: (SecondSignatureTransaction) -> Unit,
    onRunTamperRecipient: (SecondSignatureTransaction) -> Unit,
    onRunSelfApprovalTest: (SecondSignatureTransaction) -> Unit,
    onRunReplayTest: (SecondSignatureTransaction) -> Unit,
    onResetDemo: () -> Unit
) {
    val activeTx = transactions.firstOrNull {
        it.status == SecondSignatureStatus.SECOND_SIGNATURE_REQUIRED ||
                it.status == SecondSignatureStatus.PENDING
    } ?: transactions.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Security Checklist Card (Requested in user spec)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SECOND SIGNATURE PROTECTION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = TrustBlueContainer
                    ) {
                        Text(
                            text = "LIVE AUDIT ENGINE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrustBlue,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val isExecuted = activeTx?.status == SecondSignatureStatus.EXECUTED
                val isTampered = activeTx?.status == SecondSignatureStatus.TAMPER_DETECTED

                ChecklistItem(
                    label = "Transaction fingerprint created (SHA-256 Server Reconstructed)",
                    isCompleted = activeTx != null,
                    isPending = false
                )
                ChecklistItem(
                    label = "First signer verified (${activeTx?.senderName ?: "Rahul Sharma"})",
                    isCompleted = activeTx?.firstSignature != null,
                    isPending = false
                )
                ChecklistItem(
                    label = if (isExecuted) "Second signer verified (${activeTx?.secondSignature?.signerName ?: "Priya Patel"})" else "Waiting for second signer",
                    isCompleted = isExecuted,
                    isPending = !isExecuted && !isTampered
                )
                ChecklistItem(
                    label = if (isExecuted) "Transaction authorized & unlocked" else "Transaction locked in escrow",
                    isCompleted = isExecuted,
                    isPending = !isExecuted && !isTampered
                )

                if (isExecuted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SecurityGreenBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SecurityGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("✓ Both signatures verified independently", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecurityGreen)
                            Text("✓ Transaction fingerprint matched canonical state", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecurityGreen)
                            Text("✓ Both signers are different accounts", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecurityGreen)
                            Text("✓ Replay protection nonce consumed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecurityGreen)
                            Text("✓ Transaction securely authorized & executed.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecurityGreen)
                        }
                    }
                } else if (isTampered) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SecurityRedBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SecurityRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "❌ TAMPER DETECTED: Hash divergence caught by server. Funds preserved.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecurityRed,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        // Failure & Attack Simulation Suite
        Text(
            text = "Attack & Failure Simulation Vectors:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CharcoalText
        )

        if (activeTx != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Vector 1: Amount Tampering
                Surface(
                    onClick = { onRunTamperAmount(activeTx) },
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = SecurityRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simulate In-Flight Amount Tampering", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                            Text("Alters payload amount (₹85k -> ₹135k) post first signature. Triggers fingerprint mismatch.", fontSize = 10.sp, color = SlateText)
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SlateText, modifier = Modifier.size(16.dp))
                    }
                }

                // Vector 2: Recipient Tampering
                Surface(
                    onClick = { onRunTamperRecipient(activeTx) },
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonOff, contentDescription = null, tint = SecurityRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simulate Beneficiary Redirection Attack", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                            Text("Rewires recipient to malicious account. Triggers tamper detection & block.", fontSize = 10.sp, color = SlateText)
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SlateText, modifier = Modifier.size(16.dp))
                    }
                }

                // Vector 3: Self-Approval
                Surface(
                    onClick = { onRunSelfApprovalTest(activeTx) },
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = SecurityAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simulate Same-User Double Approval", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                            Text("User 1 attempts to sign as User 2. Blocked by server policy engine.", fontSize = 10.sp, color = SlateText)
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SlateText, modifier = Modifier.size(16.dp))
                    }
                }

                // Vector 4: Replay Attack
                Surface(
                    onClick = { onRunReplayTest(activeTx) },
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simulate Replay Attack", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                            Text("Attempts re-submission of consumed nonces or already authorized transfers.", fontSize = 10.sp, color = SlateText)
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SlateText, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onResetDemo,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TrustBlue),
            border = androidx.compose.foundation.BorderStroke(1.dp, TrustBlue)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset Demo Scenario (Clean Corporate Transfer)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChecklistItem(
    label: String,
    isCompleted: Boolean,
    isPending: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 3.dp)
    ) {
        Icon(
            imageVector = when {
                isCompleted -> Icons.Default.CheckCircle
                isPending -> Icons.Default.Adjust
                else -> Icons.Default.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = when {
                isCompleted -> SecurityGreen
                isPending -> SecurityAmber
                else -> SlateMuted
            },
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isCompleted || isPending) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isCompleted) CharcoalText else if (isPending) SecurityAmber else SlateMuted
        )
    }
}

@Composable
private fun DirectoryAndPolicyTab(coSigners: List<CoSigner>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Authorized Enterprise Signers:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalText)

        coSigners.forEach { signer ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(TrustBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(signer.fullName.take(1), fontWeight = FontWeight.Bold, color = TrustBlue, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(signer.fullName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                        Text(signer.role, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TrustBlue)
                        Text(signer.email, fontSize = 10.sp, color = SlateText)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text("Security Architecture Guarantees:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalText)

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = SurfaceWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PolicyPoint("1. Transaction Binding", "Both signatures bind to the canonical SHA-256 transaction fingerprint. Any in-flight modification invalidates the first signature.")
                PolicyPoint("2. Independent Authentication", "Each signer must be independently authenticated via device baseline, biometric session, or hardware key.")
                PolicyPoint("3. Anti-Self-Approval", "The server validates that User 1 cannot authorize their own transaction as User 2.")
                PolicyPoint("4. Replay Protection", "Unique nonces, transaction versions, and an atomic execution ledger prevent reuse.")
                PolicyPoint("5. Expiration Handling", "Tokens auto-expire after 10 minutes. Expired transactions cannot be executed.")
                PolicyPoint("6. Atomic State Transitions", "Prevents double-spending and race conditions via server-side locking.")
            }
        }
    }
}

@Composable
private fun PolicyPoint(title: String, desc: String) {
    Column {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
        Text(desc, fontSize = 10.sp, color = SlateText, lineHeight = 14.sp)
    }
}
