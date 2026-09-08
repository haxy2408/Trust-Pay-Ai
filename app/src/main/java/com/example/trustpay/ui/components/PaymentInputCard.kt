package com.example.trustpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trustpay.services.RiskEngineService
import com.example.trustpay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentInputCard(
    recipient: String,
    onRecipientChange: (String) -> Unit,
    amountString: String,
    onAmountChange: (String) -> Unit,
    riskFactors: RiskEngineService.RiskFactors,
    onRiskFactorsChange: (RiskEngineService.RiskFactors) -> Unit,
    onInitiatePayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quickAmounts = listOf(500, 2000, 5000, 15000, 35000, 60000)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("payment_input_card"),
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
                    imageVector = Icons.Default.Payment,
                    contentDescription = "Payment",
                    tint = TrustCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Initiate Secure UPI Transfer",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhitePure
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Beneficiary Input
            OutlinedTextField(
                value = recipient,
                onValueChange = onRecipientChange,
                label = { Text("Beneficiary VPA / Phone / Account") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recipient_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrustCyan,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = WhitePure,
                    unfocusedTextColor = SlateLight,
                    focusedLabelColor = TrustCyan,
                    unfocusedLabelColor = SlateText
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Amount Input
            OutlinedTextField(
                value = amountString,
                onValueChange = onAmountChange,
                label = { Text("Transfer Amount (₹ INR)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrustCyan,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = WhitePure,
                    unfocusedTextColor = SlateLight,
                    focusedLabelColor = TrustCyan,
                    unfocusedLabelColor = SlateText
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Amount Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickAmounts.forEach { amt ->
                    Surface(
                        onClick = { onAmountChange(amt.toString()) },
                        shape = RoundedCornerShape(8.dp),
                        color = Navy700,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Text(
                            text = "₹$amt",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SlateLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Simulated Threat Signals Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Threat Simulation",
                    tint = SlateText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Threat Simulation Vector Toggles",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateText
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Risk Toggles
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Navy700)
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                ThreatToggleRow(
                    label = "Untrusted Device (+25)",
                    description = "Hardware identifier not in enrolled keystore",
                    checked = riskFactors.untrustedDevice,
                    onCheckedChange = { onRiskFactorsChange(riskFactors.copy(untrustedDevice = it)) },
                    testTag = "toggle_untrusted_device"
                )
                ThreatToggleRow(
                    label = "New Beneficiary (+20)",
                    description = "No historical transfer ledger with this VPA",
                    checked = riskFactors.newRecipient,
                    onCheckedChange = { onRiskFactorsChange(riskFactors.copy(newRecipient = it)) },
                    testTag = "toggle_new_recipient"
                )
                ThreatToggleRow(
                    label = "Unusual Context (+20)",
                    description = "Abnormal time of day or geolocation hop",
                    checked = riskFactors.unusualContext,
                    onCheckedChange = { onRiskFactorsChange(riskFactors.copy(unusualContext = it)) },
                    testTag = "toggle_unusual_context"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Execute Button
            Button(
                onClick = onInitiatePayment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("send_payment_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TrustCyan)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Compute Risk & Authenticate",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Forward",
                        tint = Navy900,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThreatToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (checked) SecurityAmber else WhitePure
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = SlateText
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = WhitePure,
                checkedTrackColor = SecurityAmber,
                uncheckedThumbColor = SlateText,
                uncheckedTrackColor = Navy900
            )
        )
    }
}
