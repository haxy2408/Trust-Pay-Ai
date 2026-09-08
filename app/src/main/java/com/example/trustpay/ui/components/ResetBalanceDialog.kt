package com.example.trustpay.ui.components

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
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Navy800),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("reset_balance_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = TrustCyan,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Reset Demo Balance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhitePure
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Select a preset or enter a custom liquidity amount for testing:",
                    fontSize = 12.sp,
                    color = SlateLight
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(25000, 50000, 100000).forEach { preset ->
                        Surface(
                            onClick = { amountInput = preset.toString() },
                            shape = RoundedCornerShape(8.dp),
                            color = Navy700,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "₹${preset / 1000}k",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WhitePure,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                        focusedBorderColor = TrustCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = WhitePure,
                        unfocusedTextColor = SlateLight
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val parsed = amountInput.toDoubleOrNull() ?: 100000.0
                            onConfirm(parsed)
                        },
                        modifier = Modifier.weight(1f).testTag("confirm_reset_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TrustCyan)
                    ) {
                        Text("Reset", color = Navy900, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
