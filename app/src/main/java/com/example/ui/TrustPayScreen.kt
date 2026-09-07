package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.example.model.AuditLogEntry
import com.example.model.formatIndianCurrency
import com.example.model.AuditStatus
import com.example.model.ChallengeType
import com.example.model.FinalDecisionStatus
import com.example.model.PaymentTransaction
import com.example.model.VerificationStep
import com.example.security.BiometricAuthManager
import com.example.ui.theme.NavyContainer
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.OnNavyContainer
import com.example.ui.theme.OnSecurityAmber
import com.example.ui.theme.OnSecurityGreen
import com.example.ui.theme.OnSecurityRed
import com.example.ui.theme.OnTrustCyan
import com.example.ui.theme.SecurityAmber
import com.example.ui.theme.SecurityAmberContainer
import com.example.ui.theme.SecurityGreen
import com.example.ui.theme.SecurityGreenContainer
import com.example.ui.theme.SecurityRed
import com.example.ui.theme.SecurityRedContainer
import com.example.ui.theme.TrustCyan
import com.example.ui.theme.TrustCyanContainer
import com.example.viewmodel.TrustPayUiState
import com.example.viewmodel.TrustPayViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustPayScreen(
    viewModel: TrustPayViewModel,
    biometricManager: BiometricAuthManager,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var showScenarioDialog by remember { mutableStateOf(false) }
    var showBiometricFallbackDialog by remember { mutableStateOf(false) }
    var showResetBalanceDialog by remember { mutableStateOf(false) }

    // Helper to launch biometric authentication
    val launchBiometricAuth = {
        val designatedFinger = uiState.verificationState?.designatedFinger
        if (activity != null) {
            val availability = biometricManager.checkBiometricAvailability()
            if (availability == BiometricAuthManager.BiometricStatus.AVAILABLE) {
                val subtitle = if (designatedFinger != null) {
                    "Scan: $designatedFinger (Random Challenge)"
                } else {
                    "Transaction Authentication"
                }
                val description = if (designatedFinger != null) {
                    "High-Frequency Defense Policy: Scan your $designatedFinger on sensor to authorize ₹${uiState.amountInput} to ${uiState.recipientInput}"
                } else {
                    "Authenticate using fingerprint, face, or secure screen lock to authorize ₹${uiState.amountInput} to ${uiState.recipientInput}"
                }
                biometricManager.showBiometricPrompt(
                    activity = activity,
                    title = "TrustPay AI Device Verification",
                    subtitle = subtitle,
                    description = description,
                    onSuccess = { viewModel.onBiometricSuccess() },
                    onError = { code, errString ->
                        // If user cancelled, fail per rule 7
                        viewModel.onBiometricFailure(errString)
                    },
                    onFailed = {
                        viewModel.onBiometricFailure("Biometric recognition failed")
                    }
                )
            } else {
                // Device has no enrolled lock or hardware in virtual environment
                showBiometricFallbackDialog = true
            }
        } else {
            showBiometricFallbackDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (uiState.isDarkTheme) MaterialTheme.colorScheme.surfaceVariant else NavyPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "TrustPay AI Shield",
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
                                    color = if (uiState.isDarkTheme) Color.White else NavyPrimary
                                )
                            )
                            Text(
                                text = "Transaction Authentication Engine",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::toggleTheme,
                        modifier = Modifier.testTag("btn_theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (uiState.isDarkTheme) "Switch to Navy-Blue Light Mode" else "Switch to High-Contrast Dark Security Mode",
                            tint = if (uiState.isDarkTheme) SecurityAmber else NavySecondary
                        )
                    }
                    IconButton(
                        onClick = { showScenarioDialog = true },
                        modifier = Modifier.testTag("btn_help_scenario")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Demo Scenario Guide",
                            tint = if (uiState.isDarkTheme) TrustCyan else NavySecondary
                        )
                    }
                    IconButton(
                        onClick = viewModel::logoutUser,
                        modifier = Modifier.testTag("btn_logout")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = if (uiState.isDarkTheme) SecurityRed else NavySecondary
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header Banner
            item {
                HeaderBanner(
                    isDarkTheme = uiState.isDarkTheme,
                    currentUser = uiState.currentUser,
                    onToggleTheme = viewModel::toggleTheme,
                    onOpenGuide = { showScenarioDialog = true },
                    onLogout = viewModel::logoutUser
                )
            }

            // Prominent Demo Account Balance Card (Top of Dashboard)
            item {
                AccountBalanceCard(
                    balance = uiState.accountBalance,
                    isDarkTheme = uiState.isDarkTheme,
                    onResetDemoBalance = { showResetBalanceDialog = true }
                )
            }

            // Payment Input Form Card
            item {
                PaymentInputCard(
                    uiState = uiState,
                    onRecipientChanged = viewModel::onRecipientChanged,
                    onAmountChanged = viewModel::onAmountChanged,
                    onToggleUntrustedDevice = viewModel::toggleUntrustedDevice,
                    onToggleNewRecipient = viewModel::toggleNewRecipient,
                    onToggleUnusualContext = viewModel::toggleUnusualContext,
                    onAnalyseAndPay = { viewModel.analyseAndPay(launchBiometricAuth) },
                    onTamperAttack = viewModel::triggerTamperAttackDemo,
                    onTamperRecipientAttack = viewModel::triggerPostAuthTamperRecipientDemo,
                    onResetHistory = viewModel::resetDemoHistory,
                    onPrefill = viewModel::prefillScenario
                )
            }

            // Risk Assessment & Cryptographic Binding Card
            if (uiState.currentRiskScore != null || uiState.finalDecision != FinalDecisionStatus.NONE) {
                item {
                    RiskAssessmentCard(uiState = uiState)
                }
            }

            // Rolling 5-Minute Velocity Monitor Card
            item {
                VelocityMonitorCard(
                    uiState = uiState,
                    onResetHistory = viewModel::resetDemoHistory
                )
            }

            // Successful Transactions Ledger
            item {
                SuccessfulTransactionHistoryCard(
                    transactions = uiState.allTransactions,
                    isDarkTheme = uiState.isDarkTheme
                )
            }

            // Audit Trail / Security Log
            item {
                AuditLogSection(
                    logs = uiState.auditLogs,
                    onClearLogs = viewModel::clearAuditLogs
                )
            }

            // Security Compliance Disclaimer
            item {
                SecurityDisclaimer()
            }
        }
    }

    // Modal for 3-Step Verification or High-Value Biometric
    uiState.verificationState?.let { verification ->
        VerificationFlowDialog(
            verification = verification,
            riskScore = uiState.currentRiskScore ?: 75,
            onTriggerBiometric = launchBiometricAuth,
            onChallengeInputChanged = viewModel::onChallengeInputChanged,
            onSubmitChallenge = viewModel::submitChallenge,
            onOtpInputChanged = viewModel::onOtpInputChanged,
            onSubmitOtp = viewModel::submitDemoOtp,
            onCancel = viewModel::cancelVerification
        )
    }

    // Confirmation Dialog for Demo Balance Reset (Judges)
    if (showResetBalanceDialog) {
        AlertDialog(
            onDismissRequest = { showResetBalanceDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = "Reset Demo Balance",
                    tint = TrustCyan,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Reset Demo Balance?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (uiState.isDarkTheme) Color.White else NavyPrimary
                )
            },
            text = {
                Text(
                    text = "This will restore your Demo Account Balance to ₹1,00,000.00 and clear the balance-related transaction history for judge evaluation. Your registered credentials, biometrics, and security rules will remain intact.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDemoBalance()
                        showResetBalanceDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("btn_confirm_reset_balance")
                ) {
                    Text("Reset to ₹1,00,000.00", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetBalanceDialog = false },
                    modifier = Modifier.testTag("btn_cancel_reset_balance")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("dialog_reset_demo_balance")
        )
    }

    // Fallback dialog if device/emulator lacks biometric lock
    if (showBiometricFallbackDialog) {
        val designatedFinger = uiState.verificationState?.designatedFinger
        AlertDialog(
            onDismissRequest = {
                showBiometricFallbackDialog = false
                viewModel.onBiometricFailure("Biometric prompt dismissed")
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = if (designatedFinger != null) OnSecurityAmber else TrustCyan,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (designatedFinger != null) "Random Fingerprint Challenge" else "Android Biometric Simulation",
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (designatedFinger != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SecurityAmberContainer.copy(alpha = 0.5f),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(OnSecurityAmber)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "HIGH-FREQUENCY MONEY TRANSFER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OnSecurityAmber,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Security Policy Challenge:",
                                    fontSize = 11.sp,
                                    color = NavyPrimary
                                )
                                Text(
                                    text = designatedFinger,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NavyPrimary
                                )
                            }
                        }
                        Text(
                            text = "A dynamic random fingerprint was designated to prevent automated replay. Simulate presenting the designated finger, presenting the wrong finger, or cancel.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            text = "No enrolled fingerprint or lockscreen PIN was detected on this virtual device / emulator.\n\nFor hackathon testing, would you like to simulate successful Android Biometric/Device verification, or cancel?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBiometricFallbackDialog = false
                        viewModel.onBiometricSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier.testTag("btn_simulate_biometric_pass")
                ) {
                    Text(if (designatedFinger != null) "Present $designatedFinger (Pass)" else "Simulate Biometric Pass")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (designatedFinger != null) {
                        OutlinedButton(
                            onClick = {
                                showBiometricFallbackDialog = false
                                viewModel.onBiometricFailure("Wrong finger presented on sensor (Challenge required: $designatedFinger)")
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SecurityRed),
                            modifier = Modifier.testTag("btn_simulate_wrong_finger")
                        ) {
                            Text("Wrong Finger (Fail)")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            showBiometricFallbackDialog = false
                            viewModel.onBiometricFailure("Device lockscreen unavailable / cancelled")
                        },
                        modifier = Modifier.testTag("btn_simulate_biometric_cancel")
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Demo Scenario Instructions Dialog
    if (showScenarioDialog) {
        DemoScenarioGuideDialog(onDismiss = { showScenarioDialog = false })
    }
}

/**
 * Top branding banner with tagline, theme mode switch, and hackathon sandbox badge.
 */
@Composable
private fun HeaderBanner(
    isDarkTheme: Boolean,
    currentUser: com.example.model.UserAccount? = null,
    onToggleTheme: () -> Unit,
    onOpenGuide: () -> Unit,
    onLogout: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant else NavyPrimary
        ),
        border = if (isDarkTheme) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(TrustCyan.copy(alpha = 0.5f))
        ) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = TrustCyan.copy(alpha = 0.2f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TrustCyan))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SecurityGreen)
                        )
                        Text(
                            text = "HACKATHON DEMO • SIMULATED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                TextButton(
                    onClick = onOpenGuide,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Scenario Walkthrough",
                        color = TrustCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "“Authenticate the transaction, not just the person.”",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.White,
                    lineHeight = 22.sp
                )
            )

            Text(
                text = "Cryptographically binds transaction details with SHA-256 and detects velocity anomalies across rolling 5-minute windows with 3-step verification.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.85f)
                )
            )

            // High-Contrast Theme Switcher Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDarkTheme) Color(0xFF0C1626) else Color(0x33FFFFFF),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isDarkTheme) TrustCyan.copy(alpha = 0.6f) else Color(0x66FFFFFF)
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleTheme)
                    .testTag("banner_theme_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (isDarkTheme) TrustCyan else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = if (isDarkTheme) "DARK SECURITY MODE" else "NAVY-BLUE LIGHT MODE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) TrustCyan else Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (isDarkTheme) "Accessible WCAG AAA Green & Red status tokens active" else "Clean navy-blue financial security canvas",
                                fontSize = 10.sp,
                                color = if (isDarkTheme) Color(0xFFCBD5E1) else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme() },
                        modifier = Modifier.testTag("switch_theme_mode"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TrustCyan,
                            checkedTrackColor = Color(0xFF0C4A6E),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0x55FFFFFF)
                        )
                    )
                }
            }

            // Active User Profile Bar
            if (currentUser != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkTheme) Color(0xFF0C1626) else Color(0x33FFFFFF),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isDarkTheme) TrustCyan.copy(alpha = 0.4f) else Color(0x55FFFFFF)
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TrustCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "AUTHENTICATED USER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TrustCyan,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${currentUser.fullName} • ${currentUser.email}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                        TextButton(
                            onClick = onLogout,
                            modifier = Modifier.testTag("btn_header_logout"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Logout",
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Payment Input Form Card with Quick Pre-fills and Risk Checkboxes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaymentInputCard(
    uiState: TrustPayUiState,
    onRecipientChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onToggleUntrustedDevice: (Boolean) -> Unit,
    onToggleNewRecipient: (Boolean) -> Unit,
    onToggleUnusualContext: (Boolean) -> Unit,
    onAnalyseAndPay: () -> Unit,
    onTamperAttack: () -> Unit,
    onTamperRecipientAttack: () -> Unit = {},
    onResetHistory: () -> Unit,
    onPrefill: (String, String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = NavySecondary
                )
                Text(
                    text = "Initiate Payment",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                )
            }

            // Recipient UPI ID Input
            OutlinedTextField(
                value = uiState.recipientInput,
                onValueChange = onRecipientChanged,
                label = { Text("Recipient UPI ID") },
                placeholder = { Text("e.g. rahul@okhdfcbank") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = NavySecondary
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_recipient"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavySecondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Amount Input
            OutlinedTextField(
                value = uiState.amountInput,
                onValueChange = onAmountChanged,
                label = { Text("Amount (INR)") },
                placeholder = { Text("5000") },
                leadingIcon = {
                    Text(
                        text = "₹",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavySecondary,
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_amount"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavySecondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Quick Pre-set Chips for easy judging and demonstration
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Quick Demo Scenarios:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickChip(
                        label = "₹5,000 (Velocity Demo)",
                        onClick = { onPrefill("rahul@okhdfcbank", "5000") }
                    )
                    QuickChip(
                        label = "₹45,000 (High-Value)",
                        onClick = { onPrefill("merchant@upi", "45000") }
                    )
                    QuickChip(
                        label = "₹2,500 (Low-Risk)",
                        onClick = { onPrefill("store@paytm", "2500") }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            // Optional Risk Checkboxes
            Text(
                text = "Simulated Risk Signals (Optional):",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RiskCheckboxRow(
                    title = "New / Untrusted Device",
                    subtitle = "Simulates hardware fingerprint anomaly (+25 Risk)",
                    checked = uiState.untrustedDevice,
                    onCheckedChange = onToggleUntrustedDevice,
                    testTag = "check_untrusted_device"
                )
                RiskCheckboxRow(
                    title = "New Recipient",
                    subtitle = "Simulates first-time unverified beneficiary (+20 Risk)",
                    checked = uiState.newRecipient,
                    onCheckedChange = onToggleNewRecipient,
                    testTag = "check_new_recipient"
                )
                RiskCheckboxRow(
                    title = "Unusual Time / Location",
                    subtitle = "Simulates geo-jump or midnight execution (+20 Risk)",
                    checked = uiState.unusualContext,
                    onCheckedChange = onToggleUnusualContext,
                    testTag = "check_unusual_context"
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            // 1. Analyse & Pay
            Button(
                onClick = onAnalyseAndPay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_analyse_pay"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Analyse & Pay",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // 2. Attack Demo: Tamper Amount (Post-Auth Tampering Detection Demo)
            Button(
                onClick = onTamperAttack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_tamper_attack"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecurityRedContainer,
                    contentColor = OnSecurityRed
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(SecurityRed)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = SecurityRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Attack Demo: Tamper Amount",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = OnSecurityRed
                    )
                )
            }

            // Optional Payee Tamper Demo
            OutlinedButton(
                onClick = onTamperRecipientAttack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("btn_tamper_recipient"),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(SecurityRed.copy(alpha = 0.5f))
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = SecurityRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Attack Demo: Tamper Recipient UPI",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = SecurityRed
                    )
                )
            }

            // 3. Reset Demo History
            OutlinedButton(
                onClick = onResetHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("btn_reset_history"),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(NavySecondary)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = NavySecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reset Demo History",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = NavySecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NavyContainer,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = OnNavyContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun RiskCheckboxRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = NavySecondary,
                checkmarkColor = Color.White
            ),
            modifier = Modifier.testTag(testTag)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = NavyPrimary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }
    }
}

/**
 * Visual Risk Assessment Card: Score Gauge, Signals, and Cryptographic SHA-256 Proof.
 */
@Composable
private fun RiskAssessmentCard(uiState: TrustPayUiState) {
    val score = uiState.currentRiskScore ?: 0
    val scoreProgress by animateFloatAsState(
        targetValue = (score / 100f).coerceIn(0f, 1f),
        label = "scoreProgress"
    )

    val (statusColor, containerColor, onColor, statusTitle) = when (uiState.finalDecision) {
        FinalDecisionStatus.APPROVED -> Quad(
            SecurityGreen,
            SecurityGreenContainer,
            OnSecurityGreen,
            "APPROVED"
        )
        FinalDecisionStatus.TAMPER_DETECTED -> Quad(
            SecurityRed,
            SecurityRedContainer,
            OnSecurityRed,
            "TAMPERING DETECTED"
        )
        FinalDecisionStatus.BLOCKED -> Quad(
            SecurityRed,
            SecurityRedContainer,
            OnSecurityRed,
            "PAYMENT BLOCKED"
        )
        FinalDecisionStatus.NONE -> {
            if (score >= 70) Quad(
                SecurityAmber,
                SecurityAmberContainer,
                OnSecurityAmber,
                "ELEVATED RISK / ANOMALY"
            )
            else Quad(
                SecurityGreen,
                SecurityGreenContainer,
                OnSecurityGreen,
                "LOW RISK ASSESSMENT"
            )
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(statusColor.copy(alpha = 0.6f))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row with Score & Decision Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RISK ASSESSMENT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$score",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor
                        )
                        Text(
                            text = "/100",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = containerColor,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(statusColor)
                    )
                ) {
                    Text(
                        text = statusTitle,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = onColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Animated Score Bar
            LinearProgressIndicator(
                progress = { scoreProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Decision Message
            if (uiState.decisionMessage.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = containerColor.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (uiState.finalDecision) {
                                FinalDecisionStatus.APPROVED -> Icons.Default.CheckCircle
                                FinalDecisionStatus.TAMPER_DETECTED, FinalDecisionStatus.BLOCKED -> Icons.Default.Close
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.decisionMessage,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = onColor
                            )
                        )
                    }
                }
            }

            // Detected Signals List
            if (uiState.signals.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Detected Security Signals:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    uiState.signals.forEach { signal ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = signal,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // Cryptographic SHA-256 Binding Display
            uiState.currentAnalysis?.let { analysis ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NavyDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = TrustCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "SHA-256 Payload Binding Digest",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Format: TXN_ID|RECIPIENT|AMOUNT|CURRENCY",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = analysis.boundHash,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TrustCyan,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper container for 4-tuple values.
 */
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Card displaying rolling 5-minute window stats and split-payment velocity monitor.
 */
@Composable
private fun VelocityMonitorCard(
    uiState: TrustPayUiState,
    onResetHistory: () -> Unit
) {
    val count = uiState.rollingWindowTransactions.size
    val totalAmount = uiState.rollingWindowTotalAmount
    val thresholdAmount = TrustPayViewModel.VELOCITY_AMOUNT_THRESHOLD
    val progress = (totalAmount / thresholdAmount).toFloat().coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = NavySecondary
                    )
                    Column {
                        Text(
                            text = "Velocity Anomaly Monitor",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        )
                        Text(
                            text = "Rolling 5-minute sliding window",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (count >= 2) SecurityAmberContainer else NavyContainer
                ) {
                    Text(
                        text = "$count txns in window",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (count >= 2) OnSecurityAmber else OnNavyContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Window Cumulative Total",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${String.format(Locale.US, "%,.2f", totalAmount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalAmount >= thresholdAmount) SecurityRed else NavyPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Anomaly Trigger Threshold",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "≥ 3 txns & ≥ ₹15,000",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavySecondary
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progress >= 1f) SecurityAmber else TrustCyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // List of transactions within active rolling window
            if (uiState.rollingWindowTransactions.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Text(
                    text = "Active Transactions in Rolling Window:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.rollingWindowTransactions.forEach { tx ->
                        TransactionMiniRow(tx = tx)
                    }
                }
            } else {
                Text(
                    text = "No prior transactions within the last 5 minutes. Ready for first attempt.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun TransactionMiniRow(tx: PaymentTransaction) {
    val timeStr = remember(tx.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(tx.timestamp))
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = tx.recipient,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyPrimary
                )
                Text(
                    text = "${tx.id} • $timeStr",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "₹${String.format(Locale.US, "%,.2f", tx.amount)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SecurityGreen
            )
        }
    }
}

/**
 * Audit Log Section displaying timestamped security and payment events.
 */
@Composable
private fun AuditLogSection(
    logs: List<AuditLogEntry>,
    onClearLogs: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = NavySecondary
                    )
                    Text(
                        text = "Timestamped Audit Log",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    )
                }

                if (logs.isNotEmpty()) {
                    TextButton(
                        onClick = onClearLogs,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Clear", color = NavySecondary, fontSize = 12.sp)
                    }
                }
            }

            if (logs.isEmpty()) {
                Text(
                    text = "No audit events recorded yet.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    logs.take(15).forEach { entry ->
                        AuditLogRow(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogRow(entry: AuditLogEntry) {
    val timeFormat = remember(entry.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
    }

    val (badgeColor, textColor) = when (entry.status) {
        AuditStatus.SUCCESS -> SecurityGreen to OnSecurityGreen
        AuditStatus.WARNING -> SecurityAmber to OnSecurityAmber
        AuditStatus.DANGER -> SecurityRed to OnSecurityRed
        AuditStatus.INFO -> TrustCyan to OnTrustCyan
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    )
                }

                Text(
                    text = timeFormat,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = entry.details,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            entry.hash?.let { hash ->
                Text(
                    text = "SHA-256: ${hash.take(16)}...",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TrustCyan
                )
            }
        }
    }
}

/**
 * Interactive Modal Dialog for 3-Step Verification or Biometric-Only Authentication.
 */
@Composable
private fun VerificationFlowDialog(
    verification: com.example.model.VerificationState,
    riskScore: Int,
    onTriggerBiometric: () -> Unit,
    onChallengeInputChanged: (String) -> Unit,
    onSubmitChallenge: () -> Unit,
    onOtpInputChanged: (String) -> Unit,
    onSubmitOtp: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SecurityAmberContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = OnSecurityAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (verification.isVelocityAnomaly)
                                    "VELOCITY ANOMALY"
                                else
                                    "HIGH-VALUE VERIFICATION",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (verification.isVelocityAnomaly) OnSecurityAmber else NavyPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = if (verification.isVelocityAnomaly)
                                    "3-Step Verification Required"
                                else
                                    "Android Biometric Authorization",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel verification",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Transaction recap
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Authorizing Payment",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = verification.recipient,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        }
                        Text(
                            text = "₹${String.format(Locale.US, "%,.2f", verification.amount)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = NavyPrimary
                        )
                    }
                }

                // Stepper Progress Indicators (if 3-Step)
                if (verification.isVelocityAnomaly) {
                    StepperHeader(currentStep = verification.currentStep)
                }

                // Step Body
                when (verification.currentStep) {
                    VerificationStep.BIOMETRIC -> {
                        BiometricStepView(
                            designatedFinger = verification.designatedFinger,
                            onTriggerBiometric = onTriggerBiometric
                        )
                    }
                    VerificationStep.CHALLENGE -> {
                        ChallengeStepView(
                            challenge = verification.challengeData,
                            input = verification.challengeInput,
                            error = verification.challengeError,
                            onInputChanged = onChallengeInputChanged,
                            onSubmit = onSubmitChallenge
                        )
                    }
                    VerificationStep.DEMO_OTP -> {
                        DemoOtpStepView(
                            demoOtp = verification.demoOtp ?: "123456",
                            input = verification.otpInput,
                            error = verification.otpError,
                            onInputChanged = onOtpInputChanged,
                            onSubmit = onSubmitOtp
                        )
                    }
                    else -> Unit
                }

                // Cancel verification button (Rule 7: blocks payment)
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_cancel_verification"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel Verification (Blocks Payment)")
                }
            }
        }
    }
}

@Composable
private fun StepperHeader(currentStep: VerificationStep) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepPill(number = "1", label = "Biometric", isActive = currentStep == VerificationStep.BIOMETRIC, isDone = currentStep > VerificationStep.BIOMETRIC)
        HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
        StepPill(number = "2", label = "Challenge", isActive = currentStep == VerificationStep.CHALLENGE, isDone = currentStep > VerificationStep.CHALLENGE)
        HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
        StepPill(number = "3", label = "Demo OTP", isActive = currentStep == VerificationStep.DEMO_OTP, isDone = currentStep > VerificationStep.DEMO_OTP)
    }
}

@Composable
private fun StepPill(number: String, label: String, isActive: Boolean, isDone: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isDone) SecurityGreen
                    else if (isActive) NavyPrimary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = number,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) NavyPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BiometricStepView(
    designatedFinger: String? = null,
    onTriggerBiometric: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (designatedFinger != null) SecurityAmberContainer else NavyContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                tint = if (designatedFinger != null) OnSecurityAmber else NavySecondary,
                modifier = Modifier.size(40.dp)
            )
        }

        Text(
            text = if (designatedFinger != null) "Step 1: Dynamic Fingerprint Challenge" else "Step 1: Android Biometric Authentication",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (designatedFinger != null) OnSecurityAmber else NavyPrimary
            )
        )

        if (designatedFinger != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SecurityAmberContainer.copy(alpha = 0.5f),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OnSecurityAmber)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "HIGH-FREQUENCY MONEY TRANSFER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        color = OnSecurityAmber
                    )
                    Text(
                        text = "Random Fingerprint Challenge:",
                        fontSize = 12.sp,
                        color = NavyPrimary
                    )
                    Text(
                        text = designatedFinger,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = NavyPrimary
                    )
                    Text(
                        text = "Present only your $designatedFinger to defeat automated replay.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = "Authenticate with your device's enrolled fingerprint, face, or secure screen lock (PIN/pattern). TrustPay AI never stores biometric data.",
                style = MaterialTheme.typography.bodySmall.copy(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Button(
            onClick = onTriggerBiometric,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_trigger_biometric"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
        ) {
            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (designatedFinger != null) "Scan $designatedFinger" else "Launch Biometric Prompt")
        }
    }
}

@Composable
private fun ChallengeStepView(
    challenge: com.example.model.ChallengeData?,
    input: String,
    error: String?,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = null,
                tint = NavySecondary
            )
            Text(
                text = "Step 2: Transaction-Detail Challenge",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            )
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = NavyContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = challenge?.question ?: "Enter transaction detail challenge:",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = OnNavyContainer
                ),
                modifier = Modifier.padding(12.dp)
            )
        }

        OutlinedTextField(
            value = input,
            onValueChange = onInputChanged,
            label = { Text("Your Answer") },
            placeholder = {
                Text(
                    if (challenge?.type == ChallengeType.AMOUNT) "e.g. 5000"
                    else "e.g. bank"
                )
            },
            singleLine = true,
            isError = error != null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_challenge_answer"),
            shape = RoundedCornerShape(10.dp)
        )

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_submit_challenge"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
        ) {
            Text("Verify Challenge")
        }
    }
}

@Composable
private fun DemoOtpStepView(
    demoOtp: String,
    input: String,
    error: String?,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = NavySecondary
            )
            Text(
                text = "Step 3: Demo Bank OTP Verification",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            )
        }

        // Demo OTP Display Card (per rule 6: displayed only as "DEMO OTP" for hackathon testing)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = TrustCyanContainer,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(TrustCyan)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "SIMULATED HACKATHON TEST OTP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnTrustCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "DEMO OTP: $demoOtp",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = NavyPrimary
                )
                Text(
                    text = "(Enter this code below to simulate SMS OTP verification)",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = onInputChanged,
            label = { Text("Enter 6-Digit Demo OTP") },
            placeholder = { Text(demoOtp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = error != null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_demo_otp"),
            shape = RoundedCornerShape(10.dp)
        )

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_submit_demo_otp"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen)
        ) {
            Text(
                text = "Verify OTP & Authorize Payment",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Demo Scenario Walkthrough Modal Dialog.
 */
@Composable
private fun DemoScenarioGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = TrustCyan
                )
                Text(
                    text = "Hackathon Demo Scenario",
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Follow these steps to demonstrate the full security pipeline:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )

                ScenarioStepItem(
                    step = "1",
                    title = "Reset Demo History",
                    desc = "Tap 'Reset Demo History' to clear local SharedPreferences storage and reset the rolling window."
                )
                ScenarioStepItem(
                    step = "2",
                    title = "Send ₹5,000 Twice",
                    desc = "Tap 'Analyse & Pay' with ₹5,000. It is low-risk and approved immediately. Repeat a second time (Total: ₹10,000)."
                )
                ScenarioStepItem(
                    step = "3",
                    title = "High Frequency & Velocity Anomaly",
                    desc = "Initiate a 3rd ₹5,000 payment within 5 mins. A dynamic random fingerprint (e.g. Left Thumb, Right Index) is assigned to defeat replay attacks, initiating 3-Step Verification."
                )
                ScenarioStepItem(
                    step = "4",
                    title = "Complete 3-Step Verification",
                    desc = "Step 1: Android Biometric (with designated finger) -> Step 2: Answer detail challenge -> Step 3: Enter the DEMO OTP."
                )
                ScenarioStepItem(
                    step = "5",
                    title = "Post-Auth Tampering Re-Verification",
                    desc = "Tap 'Attack Demo: Tamper Amount'. It demonstrates that even if biometrics succeed, re-verifying recipient, amount, currency, and txId immediately catches discrepancies and blocks with 'Transaction tampering detected'."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Got It")
            }
        }
    )
}

@Composable
private fun ScenarioStepItem(step: String, title: String, desc: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(NavySecondary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Prominent Demo Account Balance card at the top of the payment dashboard.
 * - Displays "Available Balance: ₹1,00,000.00"
 * - Clearly labeled as Demo Account Balance (Simulated local sandbox)
 * - Has "Demo Balance Reset" button for judges
 */
@Composable
private fun AccountBalanceCard(
    balance: Double,
    isDarkTheme: Boolean,
    onResetDemoBalance: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(TrustCyan.copy(alpha = 0.45f))
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_demo_account_balance")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Badge & Small Demo Balance Reset Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDarkTheme) NavyContainer else NavyPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "Demo Account Balance",
                            tint = TrustCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "DEMO ACCOUNT BALANCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isDarkTheme) TrustCyan else NavyPrimary,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Text(
                            text = "Simulated Sandbox (No real bank connection)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Small Demo Balance Reset Button for judges
                OutlinedButton(
                    onClick = onResetDemoBalance,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_demo_balance_reset"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDarkTheme) TrustCyan else NavySecondary
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isDarkTheme) TrustCyan.copy(alpha = 0.5f) else NavySecondary.copy(alpha = 0.4f)
                        )
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Demo Balance Reset",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Demo Balance Reset",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Main Balance Display Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDarkTheme) Color.Black.copy(alpha = 0.35f) else NavyContainer.copy(alpha = 0.45f),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(TrustCyan.copy(alpha = 0.25f))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Available Balance: ${formatIndianCurrency(balance)}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) Color.White else NavyPrimary,
                            fontSize = 22.sp
                        ),
                        modifier = Modifier.testTag("text_available_balance")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = SecurityGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Default: ₹1,00,000.00 • Local storage • Protected by TrustPay Shield",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simple transaction-history section showing successful transactions only:
 * - Date and time
 * - Recipient
 * - Amount
 * - Balance remaining after payment
 * - Status: Successful
 */
@Composable
private fun SuccessfulTransactionHistoryCard(
    transactions: List<PaymentTransaction>,
    isDarkTheme: Boolean
) {
    val successfulTxns = transactions.filter {
        it.status == "APPROVED" || it.status.equals("Successful", ignoreCase = true)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_successful_transaction_history")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = NavySecondary
                    )
                    Text(
                        text = "Successful Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SecurityGreenContainer,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "${successfulTxns.size} approved",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSecurityGreen
                    )
                }
            }

            if (successfulTxns.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SecurityGreen.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "No successful payments yet",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Approved payments will appear here with recipient, amount, and post-payment remaining balance.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    successfulTxns.forEach { tx ->
                        SuccessfulTransactionItem(tx = tx, isDarkTheme = isDarkTheme)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessfulTransactionItem(
    tx: PaymentTransaction,
    isDarkTheme: Boolean
) {
    val dateStr = remember(tx.timestamp) {
        try {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(tx.timestamp))
        } catch (e: Exception) {
            "Recent"
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_item_${tx.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Recipient and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tx.recipient,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color.White else NavyDark
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "- ${formatIndianCurrency(tx.amount)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isDarkTheme) TrustCyan else NavyPrimary
                    )
                )
            }

            // Row 2: Date/Time and Status: Successful Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SecurityGreenContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SecurityGreen,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Status: Successful",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSecurityGreen
                        )
                    }
                }
            }

            // Row 3: Balance remaining after payment
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = TrustCyan,
                    modifier = Modifier.size(13.dp)
                )
                val balRemainingText = if (tx.balanceAfter != null) {
                    formatIndianCurrency(tx.balanceAfter)
                } else {
                    "Recorded"
                }
                Text(
                    text = "Balance remaining after payment: $balRemainingText",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) TrustCyan else NavySecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

/**
 * Footer note indicating simulation nature of the app.
 */
@Composable
private fun SecurityDisclaimer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "TRUSTPAY AI • HACKATHON PROTOTYPE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Text(
            text = "This application is a payment security simulation demo. No connections to live banking networks, real UPI rails, credit cards, or external SMS services are made.",
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
