package com.example.trustpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trustpay.model.RiskAnalysisResult
import com.example.trustpay.ui.theme.*

@Composable
fun RiskAssessmentCard(
    riskResult: RiskAnalysisResult,
    modifier: Modifier = Modifier
) {
    val (tierLabel, tierColor, tierBg) = when {
        riskResult.score >= 65 || riskResult.isVelocityAnomaly ->
            Triple("HIGH RISK", SecurityRed, SecurityRedBg)
        riskResult.score >= 35 ->
            Triple("MODERATE RISK", SecurityAmber, SecurityAmberBg)
        else ->
            Triple("LOW RISK", SecurityGreen, SecurityGreenBg)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("risk_assessment_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header with score & badge
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
                            .background(tierBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Risk Shield",
                            tint = tierColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Real-Time Risk Engine",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = tierBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, tierColor.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("risk_tier_badge")
                ) {
                    Text(
                        text = tierLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = tierColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Score Progress Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Threat Probability Score",
                            fontSize = 12.sp,
                            color = SlateText
                        )
                        Text(
                            text = "${riskResult.score} / 100",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = tierColor,
                            modifier = Modifier.testTag("risk_score_text")
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = (riskResult.score / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = tierColor,
                        trackColor = Color(0xFFE2E8F0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cryptographic Parameter Binding Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceVariant)
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Cryptographic Seal",
                                tint = TrustBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SHA-256 Parameter Binding Digest",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CharcoalText
                            )
                        }
                        Text(
                            text = "VERIFIED SEAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrustBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = riskResult.boundHash,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TrustBlue,
                        maxLines = 1,
                        modifier = Modifier.testTag("binding_hash_preview")
                    )
                }
            }

            // Signals List
            if (riskResult.signals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Active Anomaly Signals:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CharcoalText
                )
                Spacer(modifier = Modifier.height(6.dp))
                riskResult.signals.forEach { signal ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(tierColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = signal,
                            fontSize = 12.sp,
                            color = CharcoalText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Insights Callout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(TrustBlueContainer)
                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = TrustBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = riskResult.aiInsights,
                        fontSize = 11.sp,
                        color = CharcoalText,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
