package com.example.trustpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trustpay.model.UserAccount
import com.example.trustpay.ui.theme.*

@Composable
fun HeaderBanner(
    activeUser: UserAccount?,
    pendingDualAuthCount: Int,
    onSwitchUser: () -> Unit,
    onLogout: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenReceive: () -> Unit,
    onOpenDualAuth: () -> Unit,
    onOpenDistance: () -> Unit,
    onOpenSoc: () -> Unit,
    onOpenScenarios: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Navy800)
            .padding(top = 8.dp, bottom = 12.dp)
    ) {
        // Top row: Brand + Active user pill + Switch + Logout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TrustCyan.copy(alpha = 0.2f))
                        .border(1.dp, TrustCyan, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Logo",
                        tint = TrustCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "TrustPay",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhitePure
                    )
                    Text(
                        text = "Zero-Trust Payment Security",
                        fontSize = 11.sp,
                        color = TrustCyan
                    )
                }
            }

            // User Info & Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (activeUser != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Navy700,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.testTag("user_profile_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(ElectricBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeUser.fullName.take(1),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhitePure
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeUser.fullName.substringBefore(" "),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SlateLight
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onSwitchUser,
                        modifier = Modifier.size(32.dp).testTag("switch_user_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Switch User",
                            tint = TrustCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.size(32.dp).testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = SlateText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Navigation Pills Scrollable Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionChip(
                label = "Scan & Pay",
                icon = Icons.Default.QrCodeScanner,
                color = ElectricBlue,
                onClick = onOpenScan,
                testTag = "action_scan_pay"
            )
            ActionChip(
                label = "Receive QR",
                icon = Icons.Default.QrCode,
                color = TrustCyan,
                onClick = onOpenReceive,
                testTag = "action_receive_qr"
            )
            ActionChip(
                label = "Dual Auth",
                icon = Icons.Default.VerifiedUser,
                color = if (pendingDualAuthCount > 0) SecurityAmber else SlateText,
                badge = if (pendingDualAuthCount > 0) "$pendingDualAuthCount" else null,
                onClick = onOpenDualAuth,
                testTag = "action_dual_auth"
            )
            ActionChip(
                label = "Distance Classifier",
                icon = Icons.Default.LocationOn,
                color = SecurityGreen,
                onClick = onOpenDistance,
                testTag = "action_distance"
            )
            ActionChip(
                label = "Security SOC",
                icon = Icons.Default.Analytics,
                color = SlateLight,
                onClick = onOpenSoc,
                testTag = "action_soc"
            )
            ActionChip(
                label = "Demo Guide",
                icon = Icons.Default.PlayCircle,
                color = BlueGlow,
                onClick = onOpenScenarios,
                testTag = "action_scenarios"
            )
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    badge: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Navy700,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = WhitePure
            )
            if (badge != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SecurityAmber)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                }
            }
        }
    }
}
