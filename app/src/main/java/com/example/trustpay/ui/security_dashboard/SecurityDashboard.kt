package com.example.trustpay.ui.security_dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.trustpay.ai.GeminiSecurityAnalysis
import com.example.trustpay.backend.SecurityApi
import com.example.trustpay.security.TransactionRiskEngine
import com.example.trustpay.transactions.PaymentTransactionRecord
import com.example.trustpay.ui.theme.*

@Composable
fun SecurityDashboard(
    lastTransaction: PaymentTransactionRecord?,
    aiAssessment: GeminiSecurityAnalysis.AiSecurityResponse?,
    onDismiss: () -> Unit
) {
    var showRawJson by remember { mutableStateOf(false) }
    val auditLogs = remember { SecurityApi.getAuditLogs() }

    val riskLevel = aiAssessment?.riskLevel ?: lastTransaction?.riskLevel ?: "LOW"
    val riskScore = aiAssessment?.riskScore ?: lastTransaction?.riskScore ?: 15
    val status = lastTransaction?.status ?: "SECURE — APPROVED"

    val statusColor = when (status) {
        "APPROVED", "SECURE — APPROVED" -> SecurityGreen
        "BLOCKED", "TAMPER_DETECTED" -> SecurityRed
        else -> SecurityAmber
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("security_dashboard_modal")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(TrustBlueContainer, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = TrustBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SECURITY DASHBOARD",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CharcoalText
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).testTag("close_security_dashboard")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SlateText)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Security Status Overview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceVariant,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "SECURITY STATUS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateMuted
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val checks = aiAssessment?.securityChecks
                        SecurityCheckRow("Device Recognized", checks?.get("device") == "PASS" || lastTransaction?.deviceVerified == true)
                        SecurityCheckRow("Biometric Verified", checks?.get("biometric") == "PASS" || lastTransaction?.biometricVerified == true)
                        SecurityCheckRow("Face Verified", checks?.get("face") == "PASS" || lastTransaction?.faceVerified == true)
                        SecurityCheckRow("Demo Bank OTP Verified", checks?.get("otp") == "PASS" || lastTransaction?.otpVerified == true)
                        SecurityCheckRow("Transaction Risk Checked", true)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Risk Level:", fontSize = 12.sp, color = SlateText)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (riskLevel) {
                                    "LOW" -> SecurityGreenBg
                                    "MEDIUM" -> SecurityAmberBg
                                    else -> SecurityRedBg
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when (riskLevel) {
                                        "LOW" -> SecurityGreen.copy(alpha = 0.3f)
                                        "MEDIUM" -> SecurityAmber.copy(alpha = 0.3f)
                                        else -> SecurityRed.copy(alpha = 0.3f)
                                    }
                                )
                            ) {
                                Text(
                                    text = riskLevel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (riskLevel) {
                                        "LOW" -> SecurityGreen
                                        "MEDIUM" -> SecurityAmber
                                        else -> SecurityRed
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Risk Score:", fontSize = 12.sp, color = SlateText)
                            Text(
                                text = "$riskScore/100",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Transaction Status:", fontSize = 12.sp, color = SlateText)
                            Text(
                                text = status,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }

                        val reasons = aiAssessment?.reasonCodes ?: emptyList()
                        if (reasons.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "Reasons:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateMuted)
                            reasons.forEach { r ->
                                Text(text = "• $r", fontSize = 11.sp, color = SecurityAmber, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Recommended Action:", fontSize = 12.sp, color = SlateText)
                            Text(
                                text = aiAssessment?.recommendedAction ?: "ALLOW",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (aiAssessment?.recommendedAction) {
                                    "BLOCK" -> SecurityRed
                                    "EXTRA_VERIFICATION" -> SecurityAmber
                                    else -> SecurityGreen
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Gemini AI Response Toggle
                OutlinedButton(
                    onClick = { showRawJson = !showRawJson },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth().testTag("toggle_raw_json_btn")
                ) {
                    Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp), tint = TrustBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showRawJson) "Hide Structured AI JSON" else "View Structured AI JSON Response",
                        fontSize = 12.sp,
                        color = CharcoalText,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (showRawJson && aiAssessment != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceVariant,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = aiAssessment.rawJsonOutput,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CharcoalText,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Security Audit Trail
                Text(
                    text = "Security Audit Trail (Real-Time)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText
                )

                Spacer(modifier = Modifier.height(8.dp))

                auditLogs.take(6).forEach { log ->
                    AuditItemRow(log)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun SecurityCheckRow(label: String, passed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (passed) SecurityGreen else SecurityRed,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (passed) CharcoalText else SecurityRed,
            fontWeight = if (passed) FontWeight.Normal else FontWeight.Medium
        )
    }
}

@Composable
private fun AuditItemRow(log: SecurityApi.SecurityAuditRecord) {
    val color = when (log.severity) {
        "SUCCESS" -> SecurityGreen
        "CRITICAL" -> SecurityRed
        "WARNING" -> SecurityAmber
        else -> SlateText
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = log.eventType, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                    Text(text = log.formattedTime, fontSize = 10.sp, color = SlateText)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = log.description, fontSize = 11.sp, color = SlateText, maxLines = 2)
            }
        }
    }
}
