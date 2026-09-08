package com.example.trustpay.ui.otp_screen

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.trustpay.security.DemoBankOtp
import com.example.trustpay.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(
    onOtpVerified: () -> Unit,
    onCancel: () -> Unit
) {
    var activeSession by remember { mutableStateOf(DemoBankOtp.getActiveSession()) }
    var enteredOtp by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(DemoBankOtp.getRemainingSeconds()) }

    // Live countdown timer
    LaunchedEffect(activeSession.sessionId) {
        while (true) {
            val rem = DemoBankOtp.getRemainingSeconds()
            remainingSeconds = rem
            if (rem <= 0) {
                activeSession = DemoBankOtp.generateNewOtp()
                statusMessage = "Previous OTP expired. New demo OTP generated."
                isError = false
            }
            delay(1000L)
        }
    }

    Dialog(onDismissRequest = onCancel) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("otp_screen_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
                                .size(32.dp)
                                .background(TrustBlueContainer, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pin,
                                contentDescription = null,
                                tint = TrustBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "One-Time Password",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                    }

                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(28.dp).testTag("close_otp_modal")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SlateText)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mandatory Prominent Demo Label
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SecurityAmberBg,
                    border = BorderStroke(1.dp, SecurityAmber.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = DemoBankOtp.DEMO_LABEL,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SecurityAmber,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Simulated authentication step for security testing only.",
                            fontSize = 10.sp,
                            color = SlateText,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Development Demo OTP Reveal Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceVariant,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth().testTag("demo_otp_display_box")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DEMO OTP GENERATED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrustBlue
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activeSession.demoPlainOtpForDisplay,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 6.sp,
                            color = CharcoalText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = SlateText, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Expires in ${remainingSeconds}s",
                                fontSize = 11.sp,
                                color = if (remainingSeconds <= 10) SecurityRed else SlateText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // OTP Input Field
                OutlinedTextField(
                    value = enteredOtp,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            enteredOtp = it
                        }
                    },
                    label = { Text("Enter 6-digit Code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CharcoalText,
                        unfocusedTextColor = CharcoalText,
                        focusedBorderColor = TrustBlue,
                        unfocusedBorderColor = CardBorder,
                        focusedLabelColor = TrustBlue,
                        unfocusedLabelColor = SlateMuted
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("otp_input_field")
                )

                // Quick Auto-Fill Demo OTP Button
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        enteredOtp = activeSession.demoPlainOtpForDisplay
                    },
                    modifier = Modifier.testTag("autofill_demo_otp_btn")
                ) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp), tint = TrustBlue)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-Fill Demo OTP", fontSize = 12.sp, color = TrustBlue, fontWeight = FontWeight.Bold)
                }

                statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = msg,
                        fontSize = 11.sp,
                        color = if (isError) SecurityRed else SecurityGreen,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            activeSession = DemoBankOtp.generateNewOtp()
                            enteredOtp = ""
                            statusMessage = "New OTP generated."
                            isError = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Resend", fontSize = 13.sp, color = CharcoalText, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val result = DemoBankOtp.verifyOtp(enteredOtp)
                            if (result.otpVerified) {
                                isError = false
                                statusMessage = result.message
                                onOtpVerified()
                            } else {
                                isError = true
                                statusMessage = result.message
                            }
                        },
                        enabled = enteredOtp.length == 6,
                        colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f).height(46.dp).testTag("verify_otp_submit_btn")
                    ) {
                        Text("Verify OTP", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WhitePure)
                    }
                }
            }
        }
    }
}
