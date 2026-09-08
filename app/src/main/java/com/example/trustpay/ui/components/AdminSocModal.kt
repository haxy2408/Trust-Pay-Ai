package com.example.trustpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.trustpay.model.AuditStatus
import com.example.trustpay.model.DualAuthStatus
import com.example.trustpay.security.CryptoUtils
import com.example.trustpay.storage.TrustPayStorage
import com.example.trustpay.ui.theme.*

@Composable
fun AdminSocModal(
    onDismiss: () -> Unit
) {
    val txns = TrustPayStorage.getTransactions()
    val auditLogs = TrustPayStorage.getAuditLogs()
    val dualAuths = TrustPayStorage.getDualAuthTransactions()
    val qrTokens = TrustPayStorage.getQrTokens()

    val approvedCount = txns.count { it.status == "APPROVED" }
    val blockedCount = txns.count { it.status == "BLOCKED" || it.status == "TAMPER_DETECTED" }
    val tamperCount = auditLogs.count { it.title.contains("Tamper", ignoreCase = true) }
    val velocityBreaches = auditLogs.count { it.title.contains("Velocity", ignoreCase = true) || it.details.contains("Velocity", ignoreCase = true) }
    val totalVolume = txns.filter { it.status == "APPROVED" }.sumOf { it.amount }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Navy800),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("admin_soc_modal")
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = TrustCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TrustPay Security SOC",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhitePure
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateText)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SOC Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        title = "Approved Volume",
                        value = CryptoUtils.formatIndianCurrency(totalVolume),
                        color = SecurityGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Blocked / Tamper",
                        value = "$blockedCount events",
                        color = if (blockedCount > 0) SecurityRed else SlateLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        title = "Tamper Intercepts",
                        value = "$tamperCount caught",
                        color = if (tamperCount > 0) SecurityRed else SecurityGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Velocity Spikes",
                        value = "$velocityBreaches flagged",
                        color = if (velocityBreaches > 0) SecurityAmber else SecurityGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        title = "Dual-Auth Executed",
                        value = "${dualAuths.count { it.status == DualAuthStatus.APPROVED_BY_TWO_SIGNERS }} of ${dualAuths.size}",
                        color = SecurityAmber,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Rotating QR Tokens",
                        value = "${qrTokens.size} created",
                        color = TrustCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Security Architecture Specifications Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Navy700)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Security Invariants & Protocol Guarantees:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrustCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• SHA-256 Parameter Binding seals TXN_ID|RECIPIENT|AMOUNT|CURRENCY.\n" +
                                    "• 30s Rotating P2P QR Tokens prevent replay attacks and photo theft.\n" +
                                    "• 5-Minute Velocity Anomaly Window halts brute force transfers.\n" +
                                    "• 2-Signer Corporate Dual Authorization prevents insider embezzlement.",
                            fontSize = 10.sp,
                            color = SlateLight,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Navy700)
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(text = title, fontSize = 10.sp, color = SlateText)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
