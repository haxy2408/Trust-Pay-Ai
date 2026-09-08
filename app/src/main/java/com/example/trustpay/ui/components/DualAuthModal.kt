package com.example.trustpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.trustpay.model.CoSigner
import com.example.trustpay.model.DualAuthStatus
import com.example.trustpay.model.DualAuthTransaction
import com.example.trustpay.security.CryptoUtils
import com.example.trustpay.storage.TrustPayStorage
import com.example.trustpay.ui.theme.*
import java.util.UUID

@Composable
fun DualAuthModal(
    onDismiss: () -> Unit
) {
    val activeUser = TrustPayStorage.getActiveUser()
    var selectedTab by remember { mutableStateOf(0) } // 0: Pending, 1: Initiate, 2: Directory
    var dualAuthList by remember { mutableStateOf(TrustPayStorage.getDualAuthTransactions()) }
    val coSigners = remember { TrustPayStorage.getCoSigners() }

    // Initiate form state
    var recipientInput by remember { mutableStateOf("vendor.corp@icicibank") }
    var amountInput by remember { mutableStateOf("75000") }
    var noteInput by remember { mutableStateOf("Enterprise Server Infrastructure Payment") }
    var selectedCoSigner by remember { mutableStateOf(coSigners.firstOrNull()) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    fun refreshList() {
        dualAuthList = TrustPayStorage.getDualAuthTransactions()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Navy800),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp)
                .testTag("dual_auth_modal")
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = SecurityAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dual-Authorization Engine",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhitePure
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateText)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Navy700,
                    contentColor = WhitePure,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; refreshList() },
                        text = { Text("Pending (${dualAuthList.count { it.status == DualAuthStatus.PENDING_SECOND_AUTH }})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Initiate", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Directory", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        // Pending Approvals List
                        if (dualAuthList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No dual-authorization transactions.",
                                    fontSize = 13.sp,
                                    color = SlateText
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(dualAuthList) { tx ->
                                    val isPending = tx.status == DualAuthStatus.PENDING_SECOND_AUTH
                                    val canApprove = isPending && !tx.initiatorId.equals(activeUser?.email, ignoreCase = true)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Navy700)
                                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = CryptoUtils.formatIndianCurrency(tx.amount),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = WhitePure
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isPending) SecurityAmberBg else SecurityGreenBg
                                                ) {
                                                    Text(
                                                        text = if (isPending) "${tx.getRemainingSeconds()}s left" else tx.status.name,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isPending) SecurityAmber else SecurityGreen,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "To: ${tx.recipient}",
                                                fontSize = 12.sp,
                                                color = SlateLight
                                            )
                                            Text(
                                                text = "Initiated by: ${tx.initiatorName} • Designated: ${tx.coSignerName}",
                                                fontSize = 11.sp,
                                                color = SlateText
                                            )
                                            if (tx.note.isNotEmpty()) {
                                                Text(
                                                    text = "Note: ${tx.note}",
                                                    fontSize = 10.sp,
                                                    color = SlateLight
                                                )
                                            }

                                            if (isPending) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                if (tx.initiatorId.equals(activeUser?.email, ignoreCase = true)) {
                                                    Text(
                                                        text = "⚠️ Self-approval blocked. Switch active user to co-signer to authorize.",
                                                        fontSize = 10.sp,
                                                        color = SecurityAmber
                                                    )
                                                } else {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                TrustPayStorage.approveDualAuthTransaction(
                                                                    tx.id,
                                                                    activeUser?.email ?: "cosigner",
                                                                    activeUser?.fullName ?: "CoSigner"
                                                                )
                                                                refreshList()
                                                            },
                                                            modifier = Modifier.weight(1f).height(36.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text("Approve & Sign", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                                        }

                                                        OutlinedButton(
                                                            onClick = {
                                                                TrustPayStorage.rejectDualAuthTransaction(tx.id, "Rejected by second signer")
                                                                refreshList()
                                                            },
                                                            modifier = Modifier.weight(1f).height(36.dp),
                                                            shape = RoundedCornerShape(6.dp),
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SecurityRed),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, SecurityRed)
                                                        ) {
                                                            Text("Reject", fontSize = 11.sp)
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
                    1 -> {
                        // Initiate Dual Auth Form
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = recipientInput,
                                onValueChange = { recipientInput = it },
                                label = { Text("Corporate Beneficiary VPA") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TrustCyan,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = WhitePure,
                                    unfocusedTextColor = SlateLight
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { amountInput = it },
                                label = { Text("Amount (₹ INR)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TrustCyan,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = WhitePure,
                                    unfocusedTextColor = SlateLight
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = noteInput,
                                onValueChange = { noteInput = it },
                                label = { Text("Transfer Purpose Note") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TrustCyan,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = WhitePure,
                                    unfocusedTextColor = SlateLight
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Text("Select Co-Signer:", fontSize = 12.sp, color = SlateLight)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                coSigners.forEach { signer ->
                                    val isSelected = selectedCoSigner?.id == signer.id
                                    Surface(
                                        onClick = { selectedCoSigner = signer },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) ElectricBlue else Navy700,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = signer.fullName.substringBefore(" "),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WhitePure,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            if (feedbackMessage != null) {
                                Text(feedbackMessage!!, color = SecurityGreen, fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = {
                                    val amt = amountInput.toDoubleOrNull() ?: 50000.0
                                    val signer = selectedCoSigner ?: coSigners.first()
                                    val txId = "DUAL_${UUID.randomUUID().toString().take(8).uppercase()}"
                                    val nonce = UUID.randomUUID().toString().take(12)
                                    val sig1 = CryptoUtils.computeBindingHash(txId, recipientInput, amt)

                                    val tx = DualAuthTransaction(
                                        id = txId,
                                        initiatorId = activeUser?.email ?: "user1",
                                        initiatorName = activeUser?.fullName ?: "User 1",
                                        coSignerId = signer.email,
                                        coSignerName = signer.fullName,
                                        recipient = recipientInput,
                                        amount = amt,
                                        note = noteInput,
                                        firstApprovalSignature = sig1,
                                        nonce = nonce
                                    )

                                    TrustPayStorage.addDualAuthTransaction(tx)
                                    feedbackMessage = "Dual-auth initiated! Requires 2nd signature."
                                    selectedTab = 0
                                    refreshList()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SecurityAmber)
                            ) {
                                Text("Initiate Dual-Authorization", color = Navy900, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    2 -> {
                        // Directory & Policy
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Authorized Enterprise Signers:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhitePure
                            )

                            coSigners.forEach { signer ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Navy700)
                                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(signer.fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WhitePure)
                                        Text(signer.role, fontSize = 11.sp, color = TrustCyan)
                                        Text(signer.email, fontSize = 10.sp, color = SlateText)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Security Enforcement Rules:\n" +
                                        "• 2-Person Integrity: User 1 cannot approve their own transfer as User 2.\n" +
                                        "• 5-Minute Window: Pending dual-auth tokens expire automatically.\n" +
                                        "• Cryptographic Second Signature: Combined digest seals the transfer.",
                                fontSize = 11.sp,
                                color = SlateLight,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
