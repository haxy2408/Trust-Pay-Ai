package com.example.trustpay.ui.payment_screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trustpay.security.TransactionRiskEngine
import com.example.trustpay.ui.theme.*

@Composable
fun PaymentScreen(
    recipient: String,
    onRecipientChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    availableBalance: Double,
    onInitiatePayment: () -> Unit,
    onSelectScenario: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presetAmounts = listOf("500", "2500", "15000", "45000", "65000")
    val parsedAmt = amount.toDoubleOrNull() ?: 0.0
    val isInsufficient = parsedAmt > availableBalance

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier.fillMaxWidth().testTag("payment_screen_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(TrustBlueContainer, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = TrustBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Transfer Payment (Simulated)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                }

                FilledTonalButton(
                    onClick = onSelectScenario,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = TrustBlueContainer,
                        contentColor = TrustBlue
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).testTag("select_test_scenario_btn")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Presets", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recipient Input
            OutlinedTextField(
                value = recipient,
                onValueChange = onRecipientChange,
                label = { Text("Beneficiary VPA / UPI ID") },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = SlateText)
                },
                trailingIcon = {
                    if (TransactionRiskEngine.TRUSTED_RECIPIENTS.contains(recipient.trim().lowercase())) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SecurityGreenBg,
                            border = BorderStroke(1.dp, SecurityGreen.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "TRUSTED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecurityGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CharcoalText,
                    unfocusedTextColor = CharcoalText,
                    focusedBorderColor = TrustBlue,
                    unfocusedBorderColor = CardBorder,
                    focusedLabelColor = TrustBlue,
                    unfocusedLabelColor = SlateMuted
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("payment_recipient_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("Transfer Amount (INR)") },
                singleLine = true,
                prefix = {
                    Text("₹ ", color = TrustBlue, fontWeight = FontWeight.Bold)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = isInsufficient,
                supportingText = {
                    if (isInsufficient) {
                        Text("Amount exceeds available balance (₹${"%,.2f".format(availableBalance)})", color = SecurityRed, fontSize = 11.sp)
                    } else {
                        Text("Available balance: ₹${"%,.2f".format(availableBalance)}", color = SlateText, fontSize = 11.sp)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CharcoalText,
                    unfocusedTextColor = CharcoalText,
                    focusedBorderColor = TrustBlue,
                    unfocusedBorderColor = CardBorder,
                    focusedLabelColor = TrustBlue,
                    unfocusedLabelColor = SlateMuted
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Preset Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetAmounts.forEach { preset ->
                    val isSelected = amount == preset
                    OutlinedButton(
                        onClick = { onAmountChange(preset) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSelected) TrustBlue else CardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) TrustBlueContainer else SurfaceWhite,
                            contentColor = if (isSelected) TrustBlue else CharcoalText
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp).weight(1f)
                    ) {
                        Text("₹$preset", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Three-Step Protocol Summary Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceVariant,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepSummaryItem("1. Device", Icons.Default.PhoneAndroid)
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(12.dp))
                    StepSummaryItem("2. Biometric", Icons.Default.Fingerprint)
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(12.dp))
                    StepSummaryItem("3. Face ID", Icons.Default.Face)
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(12.dp))
                    StepSummaryItem("OTP", Icons.Default.Pin)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pay Action Button
            Button(
                onClick = onInitiatePayment,
                enabled = parsedAmt > 0 && !isInsufficient,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrustBlue,
                    disabledContainerColor = SlateMuted.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("initiate_payment_button")
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = WhitePure)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verify & Authorize Transfer",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhitePure
                )
            }
        }
    }
}

@Composable
private fun StepSummaryItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = CharcoalText, fontWeight = FontWeight.Medium)
    }
}
