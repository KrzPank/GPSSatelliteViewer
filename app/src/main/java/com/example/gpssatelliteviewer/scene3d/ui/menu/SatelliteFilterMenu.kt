package com.example.gpssatelliteviewer.scene3d.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.app.theme.DarkBackground
import com.example.gpssatelliteviewer.app.theme.DarkSurface
import com.example.gpssatelliteviewer.utils.CustomCheckbox
import com.example.gpssatelliteviewer.utils.ParameterSection
import com.example.gpssatelliteviewer.utils.InfoRow
import com.example.gpssatelliteviewer.app.theme.DeselectAllButton
import com.example.gpssatelliteviewer.app.theme.TextLabelColor
import com.example.gpssatelliteviewer.app.theme.TextSecondaryColor
import com.example.gpssatelliteviewer.app.theme.SelectAllButton
import com.example.gpssatelliteviewer.app.theme.TextHintColor
import com.example.gpssatelliteviewer.data.AzElHistory
import com.example.gpssatelliteviewer.utils.Description

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteFilterMenu(
    satelliteList: List<GNSSStatusData>,
    azElHistory:  Map<String, AzElHistory>,
    selectedConstellations: MutableList<String>,
    onlyUsedInFix: Boolean,
    onOnlyUsedInFixChanged: (Boolean) -> Unit,
    showLocationMarker: Boolean,
    onShowLocationMarkerChanged: (Boolean) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val allConstellations = satelliteList.map { it.constellation }.distinct()

    Column(
        modifier = modifier
            .background(DarkBackground.copy(alpha = 0.95f))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Navigation",
            color = TextLabelColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { navController.navigate("MainScreen") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                    ) {
                        Text("Main screen", color = TextLabelColor, fontSize = 12.sp)
                    }
                }
                Text(
                    text = "Double tap on scene to open/close menu",
                    color = TextSecondaryColor,
                    fontSize = 14.sp
                )
                Text(
                    text = "Click on satellite to show info and approximate orbit",
                    color = TextSecondaryColor,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.Companion.height(10.dp))

        // Header
        Text(
            text = "Satellite Filters",
            color = TextLabelColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider()

        // Constellation Filter Section
        ParameterSection("Constellations") {
            CustomCheckbox(
                label = "Only used in fix",
                checked = onlyUsedInFix,
                onCheckedChange = onOnlyUsedInFixChanged,
                description = "Show only satellites that contribute to position calculation"
            )
            Spacer(modifier = Modifier.Companion.height(8.dp))
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        selectedConstellations.clear()
                        selectedConstellations.addAll(allConstellations)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SelectAllButton)
                ) {
                    Text("Select All", color = TextLabelColor, fontSize = 12.sp)
                }
                Button(
                    onClick = { selectedConstellations.clear() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DeselectAllButton)
                ) {
                    Text("Deselect All", color = TextLabelColor, fontSize = 12.sp)
                }
            }
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
        ParameterSection("Marker Options") {
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
                selectedConstellations.contains(sat.constellation) && (!onlyUsedInFix || sat.usedInFix)
            }
            val usedInFix = satelliteList.count { it.usedInFix }

            val orbitEntries: Map<String, AzElHistory> = azElHistory.filterValues { hist ->
                (hist.firstAz != hist.lastAz) && (hist.firstEl != hist.lastEl)
            }

            val orbitKeysList = orbitEntries.keys.sorted()
            val orbitKeysDisplay = when {
                orbitKeysList.isEmpty() -> "—"
                orbitKeysList.size <= 10 -> orbitKeysList.joinToString(", ")
                else -> orbitKeysList.take(10).joinToString(", ") + ", … (${orbitKeysList.size} total)"
            }

            InfoRow("Total Satellites", "$totalSatellites")
            InfoRow("Currently Visible", "$visibleSatellites")
            InfoRow("Used in Fix", "$usedInFix")

            allConstellations.forEach { constellation ->
                val count = satelliteList.count { it.constellation == constellation }
                InfoRow(constellation, "$count")
            }

            InfoRow("Orbit estimates (approx)", "${orbitEntries.size}")
            Description("Note: orbit calculation is ONLY an approximation derived only from first and last known azimuth and elevation for a satellite.")

            InfoRow("Satellites with orbit estimate:", "")
            Text(
                text = orbitKeysDisplay,
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondaryColor,
                fontSize = 12.sp
            )
        }
    }
}
