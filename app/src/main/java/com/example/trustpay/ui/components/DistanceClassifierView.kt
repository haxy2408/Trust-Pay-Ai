package com.example.trustpay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.trustpay.model.DistanceCategory
import com.example.trustpay.model.TargetLocation
import com.example.trustpay.services.DistanceClassifierService
import com.example.trustpay.storage.TrustPayStorage
import com.example.trustpay.ui.theme.*

@Composable
fun DistanceClassifierView(
    onDismiss: () -> Unit
) {
    var userLat by remember { mutableStateOf(19.0674) } // Mumbai BKC
    var userLon by remember { mutableStateOf(72.8687) }
    var selectedCity by remember { mutableStateOf("Mumbai") }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val locations = remember { TrustPayStorage.getLocations() }

    val classifiedLocations = remember(userLat, userLon, locations, categoryFilter) {
        val list = locations.map { loc ->
            DistanceClassifierService.classifyLocation(userLat, userLon, loc)
        }.sortedBy { it.distanceKm }

        if (categoryFilter == null) list else list.filter { it.location.category.equals(categoryFilter, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp)
                .testTag("distance_classifier_dialog")
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
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SecurityGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SecurityGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Geofencing Classifier",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateText)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // City Coordinates Preset Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        Triple("Mumbai BKC", 19.0674, 72.8687),
                        Triple("Delhi CP", 28.6304, 77.2177),
                        Triple("Bengaluru", 12.9716, 77.5946)
                    )

                    presets.forEach { (city, lat, lon) ->
                        val isSelected = selectedCity == city
                        Surface(
                            onClick = {
                                selectedCity = city
                                userLat = lat
                                userLon = lon
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SecurityGreenBg else SurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) SecurityGreen else CardBorder)
                        ) {
                            Text(
                                text = city,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) SecurityGreen else CharcoalText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Rule Legend Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceVariant)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Distance Tier Classifications:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0-10km: Usual", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SecurityGreen)
                            Text("10-20km: Medium", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SecurityAmber)
                            Text("20-30km: Far", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFEA580C))
                            Text(">30km: High", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SecurityRed)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter & Add row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Destinations (${classifiedLocations.size}):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CharcoalText
                    )

                    TextButton(
                        onClick = { showAddDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TrustBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Location", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TrustBlue)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(classifiedLocations) { item ->
                        val (indicatorColor, indicatorBg) = when (item.colorIndicator) {
                            "GREEN" -> Pair(SecurityGreen, SecurityGreenBg)
                            "YELLOW" -> Pair(SecurityAmber, SecurityAmberBg)
                            "ORANGE" -> Pair(Color(0xFFEA580C), Color(0xFFFFF7ED))
                            else -> Pair(SecurityRed, SecurityRedBg)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceVariant)
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.location.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CharcoalText
                                    )
                                    Text(
                                        text = "${item.location.category} • ${item.location.address}",
                                        fontSize = 11.sp,
                                        color = SlateText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.displayLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = indicatorColor
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = indicatorBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, indicatorColor.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = item.formattedDistance,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = indicatorColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
