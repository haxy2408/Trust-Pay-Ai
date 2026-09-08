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
import androidx.compose.ui.graphics.Color
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
    onOpenDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .border(
                width = 1.dp,
                color = CardBorder,
                shape = androidx.compose.ui.graphics.RectangleShape
            )
            .padding(top = 10.dp, bottom = 12.dp)
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
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TrustBlueContainer)
                        .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Logo",
                        tint = TrustBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "TrustPay",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                    Text(
                        text = "Zero-Trust Payment Security",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TrustBlue
                    )
                }
            }

            // User Info & Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (activeUser != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.testTag("user_profile_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(TrustBlue),
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
                                fontWeight = FontWeight.SemiBold,
                                color = CharcoalText
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onSwitchUser,
                        modifier = Modifier.size(34.dp).testTag("switch_user_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Switch User",
                            tint = TrustBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.size(34.dp).testTag("logout_button")
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

        Spacer(modifier = Modifier.height(12.dp))

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
                color = TrustBlue,
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
                label = "2nd Signature",
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
                color = SlateText,
                onClick = onOpenSoc,
                testTag = "action_soc"
            )
            ActionChip(
                label = "Security Dashboard",
                icon = Icons.Default.Shield,
                color = TrustBlue,
                onClick = onOpenDashboard,
                testTag = "action_security_dashboard"
            )
            ActionChip(
                label = "Demo Guide",
                icon = Icons.Default.PlayCircle,
                color = TrustBlueLight,
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
        shape = RoundedCornerShape(12.dp),
        color = SurfaceWhite,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
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
                color = CharcoalText
            )
            if (badge != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SecurityAmberBg)
                        .border(1.dp, SecurityAmberBorder, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecurityAmber
                    )
                }
            }
        }
    }
}
