package com.example.trustpay.ui.login_screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trustpay.authentication.LoginManager
import com.example.trustpay.backend.AuthenticationApi
import com.example.trustpay.security.DeviceRecognition
import com.example.trustpay.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: (LoginManager.UserAccount) -> Unit
) {
    var emailInput by remember { mutableStateOf("alex.pay@trustpay.demo") }
    var passwordInput by remember { mutableStateOf("Password@123") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var simulateUnrecognized by remember { mutableStateOf(DeviceRecognition.isSimulatingUnrecognizedDevice()) }

    val hardwareDeviceId = remember { DeviceRecognition.getHardwareDeviceId() }
    val hardwareDeviceName = remember { DeviceRecognition.getDeviceDisplayName() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Demo Prototype Disclaimer Banner
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SecurityAmberBg,
            border = BorderStroke(1.dp, SecurityAmber.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().testTag("demo_disclaimer_banner")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = SecurityAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "DEMO / EDUCATIONAL APPLICATION\nDoes not process real bank transactions.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecurityAmber,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // App Logo & Shield Badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TrustBlueContainer,
            border = BorderStroke(1.dp, TrustBlue.copy(alpha = 0.3f)),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "TrustPay Security Shield",
                    tint = TrustBlue,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "TrustPay",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CharcoalText,
            letterSpacing = 0.5.sp
        )

        Text(
            text = "Three-Step Zero-Trust Payment Prototype",
            fontSize = 12.sp,
            color = SlateText,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Device Recognition Status Card (Step 1 Preview)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = if (simulateUnrecognized) SecurityRed else SecurityGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hardware Device Identity",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (simulateUnrecognized) SecurityRedBg else SecurityGreenBg,
                        border = BorderStroke(
                            1.dp,
                            if (simulateUnrecognized) SecurityRed.copy(alpha = 0.3f) else SecurityGreen.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = if (simulateUnrecognized) "SIMULATING UNTRUSTED" else "HARDWARE BOUND",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (simulateUnrecognized) SecurityRed else SecurityGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "ID: ${if (simulateUnrecognized) "DEV-ROGUE-ANON-9901" else hardwareDeviceId}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CharcoalText,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Model: ${if (simulateUnrecognized) "Untrusted Rogue Client" else hardwareDeviceName}",
                    fontSize = 11.sp,
                    color = SlateText
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle for easy testing of Unrecognized Device
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Simulate Unrecognized Device",
                        fontSize = 12.sp,
                        color = CharcoalText,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = simulateUnrecognized,
                        onCheckedChange = {
                            simulateUnrecognized = it
                            DeviceRecognition.setSimulateUnrecognizedDevice(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = WhitePure,
                            checkedTrackColor = SecurityRed,
                            uncheckedThumbColor = SlateMuted,
                            uncheckedTrackColor = SurfaceVariant
                        ),
                        modifier = Modifier.testTag("toggle_unrecognized_device")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Login Form Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Account Sign-In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText
                )
                Text(
                    text = "Enter simulated demo credentials:",
                    fontSize = 12.sp,
                    color = SlateText
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CharcoalText,
                        unfocusedTextColor = CharcoalText,
                        focusedBorderColor = TrustBlue,
                        unfocusedBorderColor = CardBorder,
                        focusedLabelColor = TrustBlue,
                        unfocusedLabelColor = SlateMuted
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("login_email_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility",
                                tint = SlateText
                            )
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
                    modifier = Modifier.fillMaxWidth().testTag("login_password_input")
                )

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        color = SecurityRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        errorMessage = null
                        val currentDevId = if (simulateUnrecognized) "DEV-ROGUE-ANON-9901" else hardwareDeviceId
                        val response = AuthenticationApi.login(emailInput, passwordInput, currentDevId)
                        if (response.success && response.user != null) {
                            onLoginSuccess(response.user)
                        } else {
                            errorMessage = response.errorMessage ?: "Authentication failure."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrustBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("login_submit_button")
                ) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = WhitePure)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure Sign In", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WhitePure)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quick Demo Account Selector
        Text(
            text = "Select Preset Demo Profile:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = SlateText,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    emailInput = "alex.pay@trustpay.demo"
                    passwordInput = "Password@123"
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (emailInput.contains("alex")) TrustBlue else CardBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (emailInput.contains("alex")) TrustBlueContainer else SurfaceWhite
                ),
                modifier = Modifier.weight(1f).height(44.dp).testTag("quick_user_alex")
            ) {
                Text(
                    text = "Alex (Treasury)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (emailInput.contains("alex")) TrustBlue else CharcoalText
                )
            }

            OutlinedButton(
                onClick = {
                    emailInput = "priya.sharma@trustpay.demo"
                    passwordInput = "Password@123"
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (emailInput.contains("priya")) TrustBlue else CardBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (emailInput.contains("priya")) TrustBlueContainer else SurfaceWhite
                ),
                modifier = Modifier.weight(1f).height(44.dp).testTag("quick_user_priya")
            ) {
                Text(
                    text = "Priya (Ops)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (emailInput.contains("priya")) TrustBlue else CharcoalText
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
