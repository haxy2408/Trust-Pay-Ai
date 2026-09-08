package com.example.trustpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trustpay.security.CryptoUtils
import com.example.trustpay.ui.theme.*

@Composable
fun VelocityMonitorCard(
    txnCount: Int,
    cumulativeAmount: Double,
    isBreached: Boolean,
    modifier: Modifier = Modifier
) {
    val countProgress = (txnCount / 3f).coerceIn(0f, 1f)
    val amountProgress = (cumulativeAmount / 50000.0).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("velocity_monitor_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
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
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isBreached) SecurityRedBg else TrustBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Velocity",
                            tint = if (isBreached) SecurityRed else TrustBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Rolling 5m Velocity Anomaly Guard",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                }

                if (isBreached) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SecurityRedBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SecurityRedBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = SecurityRed,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "BREACHED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecurityRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar 1: Count
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Transaction Frequency (Limit: 3 / 5m)",
                        fontSize = 12.sp,
                        color = SlateText
                    )
                    Text(
                        text = "$txnCount / 3",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (txnCount >= 3) SecurityRed else CharcoalText
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = countProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (txnCount >= 3) SecurityRed else TrustBlue,
                    trackColor = Color(0xFFE2E8F0)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar 2: Amount
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cumulative Outflow (Limit: ₹50,000 / 5m)",
                        fontSize = 12.sp,
                        color = SlateText
                    )
                    Text(
                        text = CryptoUtils.formatIndianCurrency(cumulativeAmount),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cumulativeAmount >= 50000.0) SecurityRed else CharcoalText
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = amountProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (cumulativeAmount >= 50000.0) SecurityRed else TrustBlue,
                    trackColor = Color(0xFFE2E8F0)
                )
            }
        }
    }
}
