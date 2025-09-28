package com.example.gpssatelliteviewer.ui.component.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.utils.CustomCheckbox
import com.example.gpssatelliteviewer.utils.ParameterSection
import com.example.gpssatelliteviewer.utils.StatisticItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteFilterMenu(
    satelliteList: List<GNSSStatusData>,
    selectedConstellations: MutableList<String>,
    onlyUsedInFix: Boolean,
    onOnlyUsedInFixChanged: (Boolean) -> Unit,
    showLocationMarker: Boolean,
    onShowLocationMarkerChanged: (Boolean) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier.Companion
) {
    val scrollState = rememberScrollState()
    val allConstellations = satelliteList.map { it.constellation }.distinct()

    Column(
        modifier = modifier
            .background(Color(0xDD000000)) // Semi-transparent black background
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Text(
            text = "Satellite Filters",
            color = Color.Companion.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Companion.Bold
        )

        HorizontalDivider(thickness = 1.dp, color = Color.Companion.Gray)

        // Control buttons
        Row(
            modifier = Modifier.Companion.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { navController.navigate("LocationInfoScreen") },
                modifier = Modifier.Companion.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A5568)) // Muted blue-gray
            ) {
                Text("Location Info", color = Color.Companion.White, fontSize = 12.sp)
            }
        }

        // Info Section
        ParameterSection("Navigation") {
            Text(
                text = "Double tap to open/close menu",
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp
            )
        }

        // Constellation Filter Section
        ParameterSection("Constellations") {
            // Select All / Deselect All buttons
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        selectedConstellations.clear()
                        selectedConstellations.addAll(allConstellations)
                    },
                    modifier = Modifier.Companion.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF20BD28)) // Muted forest green
                ) {
                    Text("Select All", color = Color.Companion.White, fontSize = 12.sp)
                }
                Button(
                    onClick = { selectedConstellations.clear() },
                    modifier = Modifier.Companion.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBD1A1A)) // Muted dark red
                ) {
                    Text("Deselect All", color = Color.Companion.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.Companion.height(8.dp))

            // Individual constellation checkboxes
            allConstellations.forEach { constellation ->
                CustomCheckbox(
                    label = constellation,
                    checked = selectedConstellations.contains(constellation),
                    onCheckedChange = { isSelected ->
                        if (isSelected) {
                            if (!selectedConstellations.contains(constellation)) {
                                selectedConstellations.add(constellation)
                            }
                        } else {
                            selectedConstellations.remove(constellation)
                        }
                    }
                )
            }
        }

        // Filter Options Section
        ParameterSection("Filter Options") {
            CustomCheckbox(
                label = "Only satellites used in fix",
                checked = onlyUsedInFix,
                onCheckedChange = onOnlyUsedInFixChanged,
                description = "Show only satellites that contribute to position calculation"
            )
            
            CustomCheckbox(
                label = "Show location marker",
                checked = showLocationMarker,
                onCheckedChange = onShowLocationMarkerChanged,
                description = "Display your current location marker in the 3D scene"
            )
        }

        // Statistics Section
        ParameterSection("Statistics") {
            val totalSatellites = satelliteList.size
            val visibleSatellites = satelliteList.count { sat ->
                selectedConstellations.contains(sat.constellation) &&
                        (!onlyUsedInFix || sat.usedInFix)
            }
            val usedInFix = satelliteList.count { it.usedInFix }

            StatisticItem("Total Satellites", totalSatellites.toString())
            StatisticItem("Currently Visible", visibleSatellites.toString())
            StatisticItem("Used in Fix", usedInFix.toString())

            allConstellations.forEach { constellation ->
                val count = satelliteList.count { it.constellation == constellation }
                StatisticItem(constellation, count.toString())
            }
        }
    }
}