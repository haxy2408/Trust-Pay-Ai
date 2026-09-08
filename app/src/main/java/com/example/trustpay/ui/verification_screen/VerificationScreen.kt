package com.example.trustpay.ui.verification_screen

import android.app.Activity
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.example.trustpay.backend.AuthenticationApi
import com.example.trustpay.security.BiometricVerification
import com.example.trustpay.security.DeviceRecognition
import com.example.trustpay.security.FaceVerification
import com.example.trustpay.transactions.TransactionVerification
import com.example.trustpay.transactions.VerificationPhase
import com.example.trustpay.ui.theme.*

@Composable
fun VerificationScreen(
    session: TransactionVerification.VerificationSession,
    onStep1DeviceComplete: () -> Unit,
    onStep2BiometricComplete: () -> Unit,
    onStep3FaceComplete: () -> Unit,
    onProceedToOtp: () -> Unit,
    onRegisterCurrentDevice: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var currentStep by remember { mutableStateOf(session.currentPhase) }
    var biometricError by remember { mutableStateOf<String?>(null) }
    var fallbackPinInput by remember { mutableStateOf("") }
    var faceError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onCancel) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("verification_screen_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header & Stepper indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Three-Step Verification",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(28.dp).testTag("close_verification_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SlateText)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Visual Step Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepBadge(number = "1", label = "Device", isActive = currentStep == VerificationPhase.STEP_1_DEVICE, isDone = currentStep > VerificationPhase.STEP_1_DEVICE)
                    HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), color = CardBorder)
                    StepBadge(number = "2", label = "Biometric", isActive = currentStep == VerificationPhase.STEP_2_BIOMETRIC, isDone = currentStep > VerificationPhase.STEP_2_BIOMETRIC)
                    HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), color = CardBorder)
                    StepBadge(number = "3", label = "Face ID", isActive = currentStep == VerificationPhase.STEP_3_FACE, isDone = currentStep > VerificationPhase.STEP_3_FACE)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Content for Current Step
                AnimatedContent(
                    targetState = currentStep,
                    label = "VerificationStepAnimation"
                ) { target ->
                    when (target) {
                        VerificationPhase.STEP_1_DEVICE -> {
                            Step1DeviceContent(
                                session = session,
                                onContinue = {
                                    onStep1DeviceComplete()
                                    currentStep = VerificationPhase.STEP_2_BIOMETRIC
                                },
                                onRegisterDevice = { devId, devName ->
                                    onRegisterCurrentDevice(devId, devName)
                                }
                            )
                        }

                        VerificationPhase.STEP_2_BIOMETRIC -> {
                            Step2BiometricContent(
                                session = session,
                                activity = activity,
                                errorMessage = biometricError,
                                onBiometricSuccess = {
                                    biometricError = null
                                    onStep2BiometricComplete()
                                    currentStep = VerificationPhase.STEP_3_FACE
                                },
                                onBiometricFail = { err ->
                                    biometricError = err
                                }
                            )
                        }

                        VerificationPhase.STEP_3_FACE -> {
                            Step3FaceContent(
                                session = session,
                                context = context,
                                fallbackPin = fallbackPinInput,
                                onPinChange = { fallbackPinInput = it },
                                errorMessage = faceError,
                                onFaceSuccess = {
                                    faceError = null
                                    onStep3FaceComplete()
                                    onProceedToOtp()
                                },
                                onFaceFail = { err ->
                                    faceError = err
                                }
                            )
                        }

                        else -> {
                            Box(modifier = Modifier.padding(16.dp)) {
                                Text("Proceeding to OTP step...", color = SlateText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepBadge(number: String, label: String, isActive: Boolean, isDone: Boolean) {
    val bg = when {
        isDone -> SecurityGreen
        isActive -> TrustBlue
        else -> SurfaceVariant
    }
    val contentCol = when {
        isDone || isActive -> WhitePure
        else -> SlateText
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(bg, CircleShape)
                .border(1.dp, if (isActive) TrustBlue else CardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = WhitePure, modifier = Modifier.size(14.dp))
            } else {
                Text(text = number, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = contentCol)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 10.sp, color = if (isActive) TrustBlue else SlateText, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
    }
}

// ==========================================
// STEP 1: Device Recognition UI
// ==========================================
@Composable
private fun Step1DeviceContent(
    session: TransactionVerification.VerificationSession,
    onContinue: () -> Unit,
    onRegisterDevice: (String, String) -> Unit
) {
    val deviceResult = session.deviceResult ?: DeviceRecognition.evaluateDevice(null)
    var justRegistered by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (deviceResult.deviceVerified) SecurityGreenBg else SecurityAmberBg,
            border = BorderStroke(1.dp, if (deviceResult.deviceVerified) SecurityGreen.copy(alpha = 0.4f) else SecurityAmber.copy(alpha = 0.4f)),
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = if (deviceResult.deviceVerified) SecurityGreen else SecurityAmber,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "STEP 1: Device Recognition",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = CharcoalText
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Device Details Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = SurfaceVariant,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Device ID:", fontSize = 11.sp, color = SlateText)
                    Text(deviceResult.deviceId, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TrustBlue, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hardware Client:", fontSize = 11.sp, color = SlateText)
                    Text(deviceResult.deviceName, fontSize = 11.sp, color = CharcoalText, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Registration Status:", fontSize = 11.sp, color = SlateText)
                    Text(
                        text = if (deviceResult.deviceVerified || justRegistered) "REGISTERED (TRUSTED)" else "UNRECOGNIZED DEVICE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (deviceResult.deviceVerified || justRegistered) SecurityGreen else SecurityRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!deviceResult.deviceVerified && !justRegistered) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SecurityRedBg,
                border = BorderStroke(1.dp, SecurityRed.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = SecurityRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Unrecognized hardware signature. Step-up biometrics will be enforced.",
                        fontSize = 11.sp,
                        color = SecurityRed,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    onRegisterDevice(deviceResult.deviceId, deviceResult.deviceName)
                    justRegistered = true
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, TrustBlue),
                modifier = Modifier.fillMaxWidth().testTag("register_device_btn")
            ) {
                Icon(Icons.Default.AddModerator, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Register This Device (Demo Flow)", fontSize = 12.sp, color = TrustBlue, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("step1_continue_btn")
        ) {
            Text("Proceed to Step 2: Biometric", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WhitePure)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = WhitePure, modifier = Modifier.size(16.dp))
        }
    }
}

// ==========================================
// STEP 2: Biometric Authentication UI
// ==========================================
@Composable
private fun Step2BiometricContent(
    session: TransactionVerification.VerificationSession,
    activity: FragmentActivity?,
    errorMessage: String?,
    onBiometricSuccess: () -> Unit,
    onBiometricFail: (String) -> Unit
) {
    val attempts = BiometricVerification.getFailedAttempts()
    val isLocked = BiometricVerification.isLocked()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = TrustBlueContainer,
            border = BorderStroke(1.dp, TrustBlue.copy(alpha = 0.3f)),
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = TrustBlue,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "STEP 2: Biometric Verification",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = CharcoalText
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Confirm transaction intent with your secure biometric sensor.",
            fontSize = 12.sp,
            color = SlateText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Privacy Guarantee Box
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SurfaceVariant,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = SecurityGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Hardware Isolation: Raw biometrics are never collected or stored. Only the binary OS verification result is received.",
                    fontSize = 11.sp,
                    color = SlateText,
                    lineHeight = 15.sp
                )
            }
        }

        errorMessage?.let { err ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = err,
                fontSize = 11.sp,
                color = SecurityRed,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trigger native Biometric Prompt Button
        Button(
            onClick = {
                if (activity != null) {
                    BiometricVerification.showBiometricPrompt(
                        activity = activity,
                        title = "TrustPay Biometric Authorization",
                        subtitle = "Transfer ₹${"%,.2f".format(session.amount)} to ${session.recipient}",
                        onSuccess = {
                            onBiometricSuccess()
                        },
                        onError = { err ->
                            onBiometricFail(err)
                        }
                    )
                } else {
                    // Fallback simulation in Compose preview / non-activity
                    BiometricVerification.recordSuccess()
                    onBiometricSuccess()
                }
            },
            enabled = !isLocked,
            colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("trigger_biometric_prompt_btn")
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = WhitePure, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Authorize with Fingerprint", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WhitePure)
        }

        // Quick Simulated Success Button (for testing environments without enrolled biometrics)
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = {
                BiometricVerification.recordSuccess()
                onBiometricSuccess()
            },
            modifier = Modifier.testTag("simulate_biometric_success_btn")
        ) {
            Text("Simulate Verified Sensor (Demo Sandbox)", fontSize = 12.sp, color = TrustBlue, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ==========================================
// STEP 3: Face ID / Face Authentication UI
// ==========================================
@Composable
private fun Step3FaceContent(
    session: TransactionVerification.VerificationSession,
    context: Context,
    fallbackPin: String,
    onPinChange: (String) -> Unit,
    errorMessage: String?,
    onFaceSuccess: () -> Unit,
    onFaceFail: (String) -> Unit
) {
    val isHardwareFaceAvailable = remember { FaceVerification.isHardwareFaceSupported(context) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = TrustBlueContainer,
            border = BorderStroke(1.dp, TrustBlue.copy(alpha = 0.3f)),
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = TrustBlue,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "STEP 3: Face ID Authentication",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = CharcoalText
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isHardwareFaceAvailable) {
            Text(
                text = "Class 3 Hardware Face Biometrics detected.",
                fontSize = 11.sp,
                color = SecurityGreen,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    FaceVerification.evaluateFaceAuth(context, true)
                    onFaceSuccess()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("verify_face_id_btn")
            ) {
                Icon(Icons.Default.Face, contentDescription = null, tint = WhitePure, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verify Face Biometrics", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WhitePure)
            }
        } else {
            // Mandatory fallback notice requirement
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SecurityAmberBg,
                border = BorderStroke(1.dp, SecurityAmber.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("face_unavailable_banner")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = SecurityAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Face authentication unavailable on this device",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecurityAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Using approved fallback authentication method: Enter your Cryptographic Security PIN (Demo PIN: 2026).",
                        fontSize = 11.sp,
                        color = SlateText,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = fallbackPin,
                onValueChange = {
                    if (it.length <= 4 && it.all { c -> c.isDigit() }) onPinChange(it)
                },
                label = { Text("Enter 4-Digit Security PIN") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CharcoalText,
                    unfocusedTextColor = CharcoalText,
                    focusedBorderColor = TrustBlue,
                    unfocusedBorderColor = CardBorder,
                    focusedLabelColor = TrustBlue,
                    unfocusedLabelColor = SlateMuted
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("face_fallback_pin_input")
            )

            errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = err, color = SecurityRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    val result = FaceVerification.evaluateFaceAuth(context, false, fallbackPin, "2026")
                    if (result.faceVerified) {
                        onFaceSuccess()
                    } else {
                        onFaceFail("Incorrect fallback PIN. Demo PIN is 2026.")
                    }
                },
                enabled = fallbackPin.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("submit_fallback_pin_btn")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = WhitePure, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm Approved Fallback PIN", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WhitePure)
            }
        }
    }
}
