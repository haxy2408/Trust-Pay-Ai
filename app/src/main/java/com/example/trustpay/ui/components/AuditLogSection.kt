package com.example.trustpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trustpay.model.AuditLogEntry
import com.example.trustpay.model.AuditStatus
import com.example.trustpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AuditLogSection(
    logs: List<AuditLogEntry>,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<AuditStatus?>(null) }
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    val filteredLogs = remember(logs, selectedFilter) {
        if (selectedFilter == null) logs else logs.filter { it.status == selectedFilter }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audit_log_section"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Navy800),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ListAlt,
                    contentDescription = "Audit Log",
                    tint = TrustCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Zero-Trust Security Audit Log",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhitePure
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("ALL (${logs.size})", fontSize = 11.sp) }
                )
                AuditStatus.values().forEach { status ->
                    val count = logs.count { it.status == status }
                    FilterChip(
                        selected = selectedFilter == status,
                        onClick = { selectedFilter = if (selectedFilter == status) null else status },
                        label = { Text("${status.name} ($count)", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filteredLogs.take(8).forEach { log ->
                    val (badgeColor, badgeBg) = when (log.status) {
                        AuditStatus.SUCCESS -> Pair(SecurityGreen, SecurityGreenBg)
                        AuditStatus.WARNING -> Pair(SecurityAmber, SecurityAmberBg)
                        AuditStatus.DANGER -> Pair(SecurityRed, SecurityRedBg)
                        AuditStatus.INFO -> Pair(TrustCyan, Navy700)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Navy700)
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = badgeBg
                                    ) {
                                        Text(
                                            text = log.status.name,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = log.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WhitePure
                                    )
                                }
                                Text(
                                    text = timeFormat.format(Date(log.timestamp)),
                                    fontSize = 10.sp,
                                    color = SlateText
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = log.details,
                                fontSize = 11.sp,
                                color = SlateLight
                            )
                            if (log.hash != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Digest: ${log.hash.take(20)}...",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TrustCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
