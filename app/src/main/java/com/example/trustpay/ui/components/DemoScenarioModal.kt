package com.example.trustpay.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.trustpay.services.RiskEngineService
import com.example.trustpay.ui.theme.*

data class DemoScenario(
    val title: String,
    val description: String,
    val recipient: String,
    val amount: String,
    val factors: RiskEngineService.RiskFactors,
    val badge: String,
    val badgeColor: Color,
    val icon: ImageVector
)

@Composable
fun DemoScenarioModal(
    onSelectScenario: (recipient: String, amount: String, factors: RiskEngineService.RiskFactors) -> Unit,
    onDismiss: () -> Unit
) {
    val scenarios = listOf(
        DemoScenario(
            title = "1. Nominal Low-Risk Payment",
            description = "₹500 to trusted merchant from recognized hardware. Triggers instant cryptographic SHA-256 release.",
            recipient = "coffee.shop@okhdfcbank",
            amount = "500",
            factors = RiskEngineService.RiskFactors(
                untrustedDevice = false,
                newRecipient = false,
                unusualContext = false
            ),
            badge = "LOW RISK (10)",
            badgeColor = SecurityGreen,
            icon = Icons.Default.CheckCircle
        ),
        DemoScenario(
            title = "2. Moderate Risk / Biometric Step-Up",
            description = "₹15,000 to a newly added vendor. Triggers designated sensor biometric intent challenge.",
            recipient = "laptop.store@icici",
            amount = "15000",
            factors = RiskEngineService.RiskFactors(
                untrustedDevice = false,
                newRecipient = true,
                unusualContext = false
            ),
            badge = "MEDIUM RISK (50)",
            badgeColor = SecurityAmber,
            icon = Icons.Default.Fingerprint
        ),
        DemoScenario(
            title = "3. High-Risk Threat / Device Anomaly",
            description = "Transfer from unrecognized device signature with abnormal context. Enforces multi-factor verification.",
            recipient = "unknown.entity@axis",
            amount = "45000",
            factors = RiskEngineService.RiskFactors(
                untrustedDevice = true,
                newRecipient = true,
                unusualContext = true
            ),
            badge = "HIGH RISK (75)",
            badgeColor = SecurityRed,
            icon = Icons.Default.Warning
        ),
        DemoScenario(
            title = "4. Tamper Attack Simulation",
            description = "Simulates in-flight packet payload alteration. Caught immediately by SHA-256 digest divergence.",
            recipient = "merchant.vip@okaxis",
            amount = "25000",
            factors = RiskEngineService.RiskFactors(
                untrustedDevice = false,
                newRecipient = false,
                unusualContext = false
            ),
            badge = "TAMPER TEST",
            badgeColor = SecurityRed,
            icon = Icons.Default.GppBad
        ),
        DemoScenario(
            title = "5. Velocity Anomaly Trigger",
            description = "Pre-loads high-volume transfer (₹55,000) that tests the rolling 300s velocity window and OTP confirmation.",
            recipient = "supplier.corp@kotak",
            amount = "55000",
            factors = RiskEngineService.RiskFactors(
                untrustedDevice = false,
                newRecipient = false,
                unusualContext = true
            ),
            badge = "VELOCITY THRESHOLD",
            badgeColor = SecurityAmber,
            icon = Icons.Default.Speed
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("demo_scenario_modal")
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
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = TrustBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Interactive Demo Guide",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).testTag("close_scenario_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SlateText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "Select any preset security test case to populate the transaction form with calibrated risk parameters:",
                    fontSize = 12.sp,
                    color = SlateText,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                scenarios.forEachIndexed { index, item ->
                    ScenarioCard(
                        scenario = item,
                        onClick = {
                            onSelectScenario(item.recipient, item.amount, item.factors)
                            onDismiss()
                        },
                        testTag = "scenario_preset_$index"
                    )
                    if (index < scenarios.size - 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenarioCard(
    scenario: DemoScenario,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceVariant,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = scenario.icon,
                        contentDescription = null,
                        tint = scenario.badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = scenario.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CharcoalText
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (scenario.badgeColor) {
                        SecurityGreen -> SecurityGreenBg
                        SecurityAmber -> SecurityAmberBg
                        else -> SecurityRedBg
                    },
                    border = BorderStroke(1.dp, scenario.badgeColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = scenario.badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = scenario.badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = scenario.description,
                fontSize = 12.sp,
                color = SlateText,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "VPA: ${scenario.recipient}",
                    fontSize = 11.sp,
                    color = TrustBlue,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "₹${scenario.amount}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText
                )
            }
        }
    }
}
