package com.example.trustpay.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.trustpay.model.QrPaymentRequest
import com.example.trustpay.security.CryptoUtils
import com.example.trustpay.security.QrProtocolHelper
import com.example.trustpay.storage.TrustPayStorage
import com.example.trustpay.ui.theme.*
import kotlinx.coroutines.delay
import java.util.UUID

@Composable
fun ReceiveQrModal(
    onDismiss: () -> Unit
) {
    val activeUser = TrustPayStorage.getActiveUser()
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("Payment for Services") }
    var currentToken by remember { mutableStateOf<QrPaymentRequest?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var remainingSeconds by remember { mutableStateOf(30L) }

    fun generateNewToken() {
        val amt = amountInput.toDoubleOrNull()
        val tokenId = "tp_qr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val nonce = UUID.randomUUID().toString().replace("-", "").take(12)
        val now = System.currentTimeMillis()
        val expires = now + 30_000L

        val uri = QrProtocolHelper.buildPayloadUri(tokenId, expires)
        val sig = CryptoUtils.computeBindingHash(tokenId, activeUser?.email ?: "receiver", amt ?: 0.0)

        val token = QrPaymentRequest(
            tokenId = tokenId,
            receiverId = activeUser?.email ?: "receiver",
            receiverName = activeUser?.fullName ?: "Receiver",
            amount = amt,
            note = noteInput,
            nonce = nonce,
            createdAt = now,
            expiresAt = expires,
            status = "ACTIVE",
            signature = sig,
            payloadUri = uri
        )

        TrustPayStorage.saveQrToken(token)
        currentToken = token
        qrBitmap = QrProtocolHelper.generateQrBitmap(uri, 512)
        remainingSeconds = 30L
    }

    LaunchedEffect(Unit) {
        generateNewToken()
    }

    // 1-second interval ticker for 30s TTL countdown & auto-refresh
    LaunchedEffect(currentToken?.tokenId) {
        while (true) {
            delay(1000L)
            currentToken?.let { token ->
                val rem = token.getRemainingSeconds()
                remainingSeconds = rem
                if (rem <= 0) {
                    generateNewToken()
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Navy800),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("receive_qr_modal")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = TrustCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Receive Money QR",
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

                // QR Code Bitmap Container
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WhitePure)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "Monochrome QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(color = TrustCyan)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live TTL Countdown Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = TrustCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rotating token expires in ${remainingSeconds}s",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (remainingSeconds <= 5) SecurityRed else TrustCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Amount configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = {
                            amountInput = it
                            generateNewToken()
                        },
                        label = { Text("Amount (Optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrustCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = WhitePure,
                            unfocusedTextColor = SlateLight
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = { generateNewToken() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy700),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TrustCyan)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // URI Payload Preview
                currentToken?.let { token ->
                    Text(
                        text = token.payloadUri,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SlateText,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
