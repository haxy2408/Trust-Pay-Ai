package com.example.trustpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.trustpay.firebase.FirebaseRepository
import com.example.trustpay.model.PaymentTransaction
import com.example.trustpay.model.QrPaymentRequest
import com.example.trustpay.security.CryptoUtils
import com.example.trustpay.security.QrProtocolHelper
import com.example.trustpay.storage.TrustPayStorage
import com.example.trustpay.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ScanPayModal(
    onPaymentSuccess: (PaymentTransaction) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { FirebaseRepository() }
    val activeUser = TrustPayStorage.getActiveUser()

    var manualUriInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var selectedToken by remember { mutableStateOf<QrPaymentRequest?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Active tokens created by other users available for simulated scanning
    val availableTokens = remember {
        TrustPayStorage.getQrTokens().filter {
            it.isAvailable() && !it.receiverId.equals(activeUser?.email, ignoreCase = true)
        }
    }

    fun processPayload(uri: String) {
        val params = QrProtocolHelper.parsePayloadUri(uri)
        if (params == null || !params.containsKey("tokenId")) {
            validationError = "Invalid QR code format. Must follow TrustPay P2P URI specification."
            return
        }
        val tokenId = params["tokenId"]!!
        coroutineScope.launch {
            isProcessing = true
            val result = repository.validateQrToken(tokenId)
            isProcessing = false
            if (result.valid && result.request != null) {
                selectedToken = result.request
                validationError = null
            } else {
                validationError = result.error ?: "Validation failed."
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("scan_pay_modal")
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TrustBlueContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = TrustBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Scan & Pay P2P Token",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateText)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedToken == null) {
                    // Manual Payload or Active Token Picker
                    Text(
                        text = "Paste Scanned Payload URI or select active token below:",
                        fontSize = 12.sp,
                        color = SlateText
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualUriInput,
                        onValueChange = {
                            manualUriInput = it
                            validationError = null
                        },
                        placeholder = { Text("trustpay://p2p/request?tokenId=...", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scanned_uri_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrustBlue,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = CharcoalText,
                            unfocusedTextColor = CharcoalText
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { processPayload(manualUriInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TrustBlue)
                    ) {
                        Text("Validate Payload", color = WhitePure, fontWeight = FontWeight.Bold)
                    }

                    if (validationError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = validationError!!,
                            fontSize = 11.sp,
                            color = SecurityRed,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Simulated Available Tokens (${availableTokens.size}):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CharcoalText
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (availableTokens.isEmpty()) {
                        Text(
                            text = "No other active rotating tokens found. Open 'Receive QR' from another demo user or generate one.",
                            fontSize = 11.sp,
                            color = SlateText
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 180.dp)
                        ) {
                            items(availableTokens) { token ->
                                Surface(
                                    onClick = { processPayload(token.payloadUri) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = token.receiverName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CharcoalText
                                            )
                                            Text(
                                                text = "Amount: ${token.amount?.let { CryptoUtils.formatIndianCurrency(it) } ?: "Any"}",
                                                fontSize = 11.sp,
                                                color = TrustBlue,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = "${token.getRemainingSeconds()}s left",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SecurityAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Confirmed Token Details & Execute Transfer
                    val token = selectedToken!!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceVariant)
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "BENEFICIARY VERIFIED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecurityGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = token.receiverName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )
                            Text(
                                text = token.receiverId,
                                fontSize = 12.sp,
                                color = SlateText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Amount: ${token.amount?.let { CryptoUtils.formatIndianCurrency(it) } ?: "₹500.00"}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TrustBlue
                            )
                            if (token.note.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Note: ${token.note}",
                                    fontSize = 11.sp,
                                    color = SlateText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isProcessing = true
                                val amt = token.amount ?: 500.0
                                val hash = CryptoUtils.computeBindingHash(token.tokenId, token.receiverId, amt)
                                val tx = repository.completeP2PTransfer(token.tokenId, amt, hash)
                                isProcessing = false
                                onPaymentSuccess(tx)
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("confirm_p2p_payment_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = WhitePure)
                        } else {
                            Text("Confirm & Authorize Transfer", color = WhitePure, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { selectedToken = null }) {
                        Text("Scan Different Code", color = SlateText, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
