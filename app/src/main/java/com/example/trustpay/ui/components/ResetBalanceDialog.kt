package com.example.trustpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.trustpay.ui.theme.*

@Composable
fun ResetBalanceDialog(
    currentBalance: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var amountInput by remember { mutableStateOf("100000") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("reset_balance_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(TrustBlueContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = TrustBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Reset Demo Balance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Select a preset or enter a custom liquidity amount for testing:",
                    fontSize = 12.sp,
                    color = SlateText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(25000, 50000, 100000).forEach { preset ->
                        Surface(
                            onClick = { amountInput = preset.toString() },
                            shape = RoundedCornerShape(8.dp),
                            color = if (amountInput == preset.toString()) TrustBlueContainer else SurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (amountInput == preset.toString()) TrustBlue else CardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "₹${preset / 1000}k",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (amountInput == preset.toString()) TrustBlue else CharcoalText,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Balance Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_balance_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrustBlue,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = CharcoalText,
                        unfocusedTextColor = CharcoalText,
                        focusedLabelColor = TrustBlue,
                        unfocusedLabelColor = SlateMuted
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateText),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val parsed = amountInput.toDoubleOrNull() ?: 100000.0
                            onConfirm(parsed)
                        },
                        modifier = Modifier.weight(1f).testTag("confirm_reset_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TrustBlue)
                    ) {
                        Text("Reset", color = WhitePure, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
