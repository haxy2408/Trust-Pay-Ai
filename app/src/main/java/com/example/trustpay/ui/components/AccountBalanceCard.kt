package com.example.trustpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trustpay.security.CryptoUtils
import com.example.trustpay.ui.theme.*

@Composable
fun AccountBalanceCard(
    balance: Double,
    onResetBalanceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("account_balance_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TrustBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Wallet",
                            tint = TrustBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Available Demo Liquidity",
                        fontSize = 13.sp,
                        color = SlateText,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Reset Balance Button
                TextButton(
                    onClick = onResetBalanceClick,
                    modifier = Modifier.testTag("reset_balance_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = TrustBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset Balance",
                        fontSize = 12.sp,
                        color = TrustBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = CryptoUtils.formatIndianCurrency(balance),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CharcoalText,
                modifier = Modifier.testTag("balance_amount_text")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Security assurance pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SecurityGreenBg)
                    .border(1.dp, SecurityGreenBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Status",
                            tint = SecurityGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hardware Keystore Guard Active",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = CharcoalText
                        )
                    }

                    Text(
                        text = "Zero-Trust Sealed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecurityGreen
                    )
                }
            }
        }
    }
}
