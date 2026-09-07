package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.SecurityRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrustCyan
import com.example.viewmodel.TrustPayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: TrustPayViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val form = uiState.registerForm
    val focusManager = LocalFocusManager.current
    val isDark = uiState.isDarkTheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else NavyPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = TrustCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "TRUSTPAY AI",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = if (isDark) Color.White else NavyPrimary
                                )
                            )
                            Text(
                                text = "Adaptive Payment Shield",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::toggleTheme,
                        modifier = Modifier.testTag("btn_theme_toggle_reg")
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = if (isDark) SecurityAmber else NavySecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Security Badge Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else NavyPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(TrustCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = TrustCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create Secure Account",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Register to enable AI fraud detection, velocity anomaly monitoring, and salted cryptographic verification.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }
            }

            // Registration Form Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "USER REGISTRATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TrustCyan,
                            letterSpacing = 1.sp
                        )
                    )

                    // 1. Full Name
                    Column {
                        OutlinedTextField(
                            value = form.fullName,
                            onValueChange = viewModel::onRegisterFullNameChanged,
                            label = { Text("Full Name") },
                            placeholder = { Text("e.g. Rahul Sharma") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = TrustCyan)
                            },
                            isError = form.fullNameError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrustCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_register_name")
                        )
                        if (form.fullNameError != null) {
                            Text(
                                text = form.fullNameError,
                                color = SecurityRed,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                modifier = Modifier
                                    .padding(start = 8.dp, top = 4.dp)
                                    .testTag("error_register_name")
                            )
                        }
                    }

                    // 2. Email Address
                    Column {
                        OutlinedTextField(
                            value = form.email,
                            onValueChange = viewModel::onRegisterEmailChanged,
                            label = { Text("Email Address") },
                            placeholder = { Text("e.g. rahul@example.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = TrustCyan)
                            },
                            isError = form.emailError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrustCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_register_email")
                        )
                        if (form.emailError != null) {
                            Text(
                                text = form.emailError,
                                color = SecurityRed,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                modifier = Modifier
                                    .padding(start = 8.dp, top = 4.dp)
                                    .testTag("error_register_email")
                            )
                        }
                    }

                    // 3. Mobile Number
                    Column {
                        OutlinedTextField(
                            value = form.mobileNumber,
                            onValueChange = viewModel::onRegisterMobileChanged,
                            label = { Text("Mobile Number") },
                            placeholder = { Text("10-digit mobile number") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = TrustCyan)
                            },
                            prefix = { Text("+91 ", fontWeight = FontWeight.Bold) },
                            isError = form.mobileNumberError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrustCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_register_mobile")
                        )
                        if (form.mobileNumberError != null) {
                            Text(
                                text = form.mobileNumberError,
                                color = SecurityRed,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                modifier = Modifier
                                    .padding(start = 8.dp, top = 4.dp)
                                    .testTag("error_register_mobile")
                            )
                        }
                    }

                    // 4. Password
                    Column {
                        OutlinedTextField(
                            value = form.password,
                            onValueChange = viewModel::onRegisterPasswordChanged,
                            label = { Text("Password") },
                            placeholder = { Text("At least 8 characters") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = TrustCyan)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = viewModel::toggleRegisterPasswordVisibility,
                                    modifier = Modifier.testTag("btn_toggle_reg_password")
                                ) {
                                    Icon(
                                        imageVector = if (form.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = TextMuted
                                    )
                                }
                            },
                            visualTransformation = if (form.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = form.passwordError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrustCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_register_password")
                        )
                        if (form.passwordError != null) {
                            Text(
                                text = form.passwordError,
                                color = SecurityRed,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                modifier = Modifier
                                    .padding(start = 8.dp, top = 4.dp)
                                    .testTag("error_register_password")
                            )
                        }
                    }

                    // 5. Confirm Password
                    Column {
                        OutlinedTextField(
                            value = form.confirmPassword,
                            onValueChange = viewModel::onRegisterConfirmPasswordChanged,
                            label = { Text("Confirm Password") },
                            placeholder = { Text("Re-enter your password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = TrustCyan)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = viewModel::toggleRegisterConfirmPasswordVisibility,
                                    modifier = Modifier.testTag("btn_toggle_reg_confirm_password")
                                ) {
                                    Icon(
                                        imageVector = if (form.isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle confirm password visibility",
                                        tint = TextMuted
                                    )
                                }
                            },
                            visualTransformation = if (form.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = form.confirmPasswordError != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.registerUser()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrustCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_register_confirm_password")
                        )
                        if (form.confirmPasswordError != null) {
                            Text(
                                text = form.confirmPasswordError,
                                color = SecurityRed,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                modifier = Modifier
                                    .padding(start = 8.dp, top = 4.dp)
                                    .testTag("error_register_confirm_password")
                            )
                        }
                    }

                    // Security Guarantee Notice
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = SecurityGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Passwords are never saved in plain text. Securely protected with SHA-256 salted encryption.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.5.sp,
                                    color = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Register Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.registerUser()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_register")
                    ) {
                        Text(
                            text = "REGISTER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    // "Already have an account? Login" link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Already have an account? ",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                        Text(
                            text = "Login",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TrustCyan,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clickable(onClick = viewModel::navigateToLogin)
                                .testTag("link_to_login")
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
