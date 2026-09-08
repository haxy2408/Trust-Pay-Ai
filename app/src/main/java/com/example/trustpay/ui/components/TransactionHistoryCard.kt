package com.example.trustpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trustpay.model.PaymentTransaction
import com.example.trustpay.security.CryptoUtils
import com.example.trustpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionHistoryCard(
    transactions: List<PaymentTransaction>,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss dd MMM", Locale.getDefault())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_history_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Navy800),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = TrustCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recent Transactions Ledger",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhitePure
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions recorded in this session.",
                        fontSize = 13.sp,
                        color = SlateText
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    transactions.take(5).forEach { tx ->
                        val isApproved = tx.status == "APPROVED"
                        val statusColor = if (isApproved) SecurityGreen else SecurityRed
                        val statusBg = if (isApproved) SecurityGreenBg else SecurityRedBg

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Navy700)
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tx.recipient,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WhitePure
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = statusBg
                                        ) {
                                            Text(
                                                text = tx.status,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${tx.id} • ${timeFormat.format(Date(tx.timestamp))}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = SlateText
                                    )
                                    if (tx.bindingHash.isNotEmpty()) {
                                        Text(
                                            text = "SHA: ${tx.bindingHash.take(16)}...",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TrustCyan
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CryptoUtils.formatIndianCurrency(tx.amount),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isApproved) SecurityGreen else SecurityRed
                                    )
                                    if (tx.balanceAfter != null) {
                                        Text(
                                            text = "Bal: ${CryptoUtils.formatIndianCurrency(tx.balanceAfter)}",
                                            fontSize = 10.sp,
                                            color = SlateText
                                        )
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
