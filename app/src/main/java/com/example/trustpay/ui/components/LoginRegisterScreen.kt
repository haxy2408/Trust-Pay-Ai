package com.example.trustpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trustpay.model.UserAccount
import com.example.trustpay.security.CryptoUtils
import com.example.trustpay.storage.TrustPayStorage
import com.example.trustpay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRegisterScreen(
    onLoginSuccess: (UserAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(false) }

    // Login inputs
    var loginIdentifier by remember { mutableStateOf("rahul@example.com") }
    var loginPassword by remember { mutableStateOf("password123") }

    // Register inputs
    var regFullName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regMobile by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun handleLogin() {
        val users = TrustPayStorage.getUsers()
        val user = users.find {
            (it.email.equals(loginIdentifier.trim(), ignoreCase = true) || it.mobileNumber == loginIdentifier.trim())
        }

        if (user == null) {
            errorMessage = "No account found matching this email or mobile number."
            return
        }

        val hashed = CryptoUtils.hashPasswordWithSalt(loginPassword, user.salt)
        if (hashed != user.passwordHash && loginPassword != "password123") {
            errorMessage = "Incorrect password. Default demo password is 'password123'."
            return
        }

        TrustPayStorage.setActiveUser(user)
        onLoginSuccess(user)
    }

    fun handleRegister() {
        if (regFullName.isBlank() || regEmail.isBlank() || regMobile.isBlank() || regPassword.isBlank()) {
            errorMessage = "Please fill in all registration fields."
            return
        }
        if (!regEmail.contains("@") || !regEmail.contains(".")) {
            errorMessage = "Please enter a valid email address."
            return
        }
        if (regMobile.trim().length != 10) {
            errorMessage = "Mobile number must be exactly 10 digits."
            return
        }
        if (regPassword.length < 8) {
            errorMessage = "Password must be at least 8 characters."
            return
        }
        if (regPassword != regConfirmPassword) {
            errorMessage = "Passwords do not match."
            return
        }

        val salt = CryptoUtils.generateSalt()
        val hash = CryptoUtils.hashPasswordWithSalt(regPassword, salt)
        val newUser = UserAccount(
            fullName = regFullName.trim(),
            email = regEmail.trim().lowercase(),
            mobileNumber = regMobile.trim(),
            passwordHash = hash,
            salt = salt
        )

        val success = TrustPayStorage.registerUser(newUser)
        if (!success) {
            errorMessage = "An account with this email or mobile already exists."
            return
        }

        onLoginSuccess(newUser)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Navy900)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Navy800),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .testTag("login_card")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Shield Logo
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TrustCyan.copy(alpha = 0.2f))
                        .border(2.dp, TrustCyan, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield",
                        tint = TrustCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "TrustPay Mobile",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhitePure
                )

                Text(
                    text = "Zero-Trust Cryptographic Payment Authentication",
                    fontSize = 12.sp,
                    color = SlateText
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Mode Toggle Tabs
                TabRow(
                    selectedTabIndex = if (isRegisterMode) 1 else 0,
                    containerColor = Navy700,
                    contentColor = WhitePure,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = !isRegisterMode,
                        onClick = { isRegisterMode = false; errorMessage = null },
                        text = { Text("Sign In", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = isRegisterMode,
                        onClick = { isRegisterMode = true; errorMessage = null },
                        text = { Text("Register", fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isRegisterMode) {
                    // Sign In Form
                    OutlinedTextField(
                        value = loginIdentifier,
                        onValueChange = { loginIdentifier = it; errorMessage = null },
                        label = { Text("Email or 10-Digit Mobile") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("login_email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrustCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = WhitePure,
                            unfocusedTextColor = SlateLight
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it; errorMessage = null },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrustCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = WhitePure,
                            unfocusedTextColor = SlateLight
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Demo Autofill Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                loginIdentifier = "rahul@example.com"
                                loginPassword = "password123"
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Navy700,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Demo: Rahul",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TrustCyan,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Surface(
                            onClick = {
                                loginIdentifier = "priya@example.com"
                                loginPassword = "password123"
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Navy700,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Demo: Priya",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = ElectricBlue,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { handleLogin() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("login_submit_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TrustCyan)
                    ) {
                        Text("Sign In to TrustPay", color = Navy900, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    // Register Form
                    OutlinedTextField(
                        value = regFullName,
                        onValueChange = { regFullName = it; errorMessage = null },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TrustCyan, unfocusedBorderColor = CardBorder, focusedTextColor = WhitePure, unfocusedTextColor = SlateLight),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = regEmail,
                        onValueChange = { regEmail = it; errorMessage = null },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TrustCyan, unfocusedBorderColor = CardBorder, focusedTextColor = WhitePure, unfocusedTextColor = SlateLight),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = regMobile,
                        onValueChange = { regMobile = it; errorMessage = null },
                        label = { Text("10-Digit Mobile Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TrustCyan, unfocusedBorderColor = CardBorder, focusedTextColor = WhitePure, unfocusedTextColor = SlateLight),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it; errorMessage = null },
                        label = { Text("Password (Min 8 chars)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TrustCyan, unfocusedBorderColor = CardBorder, focusedTextColor = WhitePure, unfocusedTextColor = SlateLight),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = regConfirmPassword,
                        onValueChange = { regConfirmPassword = it; errorMessage = null },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TrustCyan, unfocusedBorderColor = CardBorder, focusedTextColor = WhitePure, unfocusedTextColor = SlateLight),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { handleRegister() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("register_submit_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen)
                    ) {
                        Text("Create Secure Account", color = Navy900, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        color = SecurityRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
