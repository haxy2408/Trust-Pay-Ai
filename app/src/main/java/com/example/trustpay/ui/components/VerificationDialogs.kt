package com.example.trustpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.trustpay.model.VerificationState
import com.example.trustpay.model.VerificationStep
import com.example.trustpay.security.CryptoUtils
import com.example.trustpay.ui.theme.*

@Composable
fun VerificationDialog(
    state: VerificationState,
    onBiometricSuccess: () -> Unit,
    onChallengeAnswer: (String) -> Unit,
    onOtpSubmit: (String) -> Unit,
    onSimulateTamper: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Navy800),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("verification_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state.currentStep) {
                    VerificationStep.BIOMETRIC -> {
                        BiometricStepContent(
                            state = state,
                            onAuthenticate = onBiometricSuccess,
                            onSimulateTamper = onSimulateTamper,
                            onCancel = onDismiss
                        )
                    }
                    VerificationStep.CHALLENGE -> {
                        ChallengeStepContent(
                            state = state,
                            onSubmit = onChallengeAnswer,
                            onCancel = onDismiss
                        )
                    }
                    VerificationStep.DEMO_OTP -> {
                        OtpStepContent(
                            state = state,
                            onSubmit = onOtpSubmit,
                            onCancel = onDismiss
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun BiometricStepContent(
    state: VerificationState,
    onAuthenticate: () -> Unit,
    onSimulateTamper: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(TrustCyan.copy(alpha = 0.2f))
            .border(2.dp, TrustCyan, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Fingerprint",
            tint = TrustCyan,
            modifier = Modifier.size(36.dp)
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
        text = "Biometric Intent Verification",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = WhitePure
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = "Authenticating transfer of ${CryptoUtils.formatIndianCurrency(state.amount)} to ${state.recipient}",
        fontSize = 12.sp,
        color = SlateLight,
        lineHeight = 16.sp
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Designated Finger Challenge Card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Navy700)
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "DESIGNATED SENSOR CHALLENGE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SecurityAmber
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Touch Sensor With:",
                fontSize = 11.sp,
                color = SlateText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = state.designatedFinger,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = WhitePure
            )
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Primary Biometric Confirm Button
    Button(
        onClick = onAuthenticate,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("confirm_biometric_button"),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TrustCyan)
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = null,
            tint = Navy900,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Verify Fingerprint & Sign",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Navy900
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Tamper Attack Simulation Button
    OutlinedButton(
        onClick = onSimulateTamper,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .testTag("simulate_tamper_button"),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecurityRed),
        border = androidx.compose.foundation.BorderStroke(1.dp, SecurityRed)
    ) {
        Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = null,
            tint = SecurityRed,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Simulate Tamper Attack (Test)",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = SecurityRed
        )
    }

    Spacer(modifier = Modifier.height(6.dp))

    TextButton(onClick = onCancel) {
        Text("Cancel Transaction", color = SlateText, fontSize = 12.sp)
    }
}

@Composable
private fun ChallengeStepContent(
    state: VerificationState,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit
) {
    var answer by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(SecurityAmberBg)
            .border(2.dp, SecurityAmber, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = "Challenge",
            tint = SecurityAmber,
            modifier = Modifier.size(30.dp)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Cognitive Intent Challenge",
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = WhitePure
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = state.challengeData?.question ?: "Confirm the beneficiary domain:",
        fontSize = 13.sp,
        color = SlateLight
    )

    Spacer(modifier = Modifier.height(14.dp))

    OutlinedTextField(
        value = answer,
        onValueChange = { answer = it },
        placeholder = { Text("Enter answer (e.g. ${state.challengeData?.expectedAnswer})") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("challenge_input"),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SecurityAmber,
            unfocusedBorderColor = CardBorder,
            focusedTextColor = WhitePure,
            unfocusedTextColor = SlateLight
        ),
        shape = RoundedCornerShape(10.dp)
    )

    Spacer(modifier = Modifier.height(14.dp))

    Button(
        onClick = { onSubmit(answer) },
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("submit_challenge_button"),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SecurityAmber)
    ) {
        Text("Confirm Intent", color = Navy900, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(6.dp))

    TextButton(onClick = onCancel) {
        Text("Cancel", color = SlateText, fontSize = 12.sp)
    }
}

@Composable
private fun OtpStepContent(
    state: VerificationState,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit
) {
    var otpInput by remember { mutableStateOf(state.demoOtp) }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(ElectricBlue.copy(alpha = 0.2f))
            .border(2.dp, ElectricBlue, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Sms,
            contentDescription = "OTP",
            tint = ElectricBlue,
            modifier = Modifier.size(30.dp)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "6-Digit Secure OTP Verification",
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = WhitePure
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = "High-risk velocity breach triggered. Demo OTP prefilled:",
        fontSize = 12.sp,
        color = SlateLight
    )

    Spacer(modifier = Modifier.height(14.dp))

    OutlinedTextField(
        value = otpInput,
        onValueChange = { if (it.length <= 6) otpInput = it },
        label = { Text("6-Digit OTP") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("otp_input"),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TrustCyan,
            unfocusedBorderColor = CardBorder,
            focusedTextColor = WhitePure,
            unfocusedTextColor = SlateLight
        ),
        shape = RoundedCornerShape(10.dp)
    )

    Spacer(modifier = Modifier.height(14.dp))

    Button(
        onClick = { onSubmit(otpInput) },
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("submit_otp_button"),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TrustCyan)
    ) {
        Text("Verify & Release Funds", color = Navy900, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(6.dp))

    TextButton(onClick = onCancel) {
        Text("Cancel", color = SlateText, fontSize = 12.sp)
    }
}
