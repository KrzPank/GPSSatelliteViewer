package com.example.gnssmap.scene3d.ui.menu

import android.util.Log
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gnssmap.data.GNSSStatusData
import com.example.gnssmap.app.theme.DarkBackgroundColor
import com.example.gnssmap.app.theme.DarkSurfaceColor
import com.example.gnssmap.utils.CustomCheckbox
import com.example.gnssmap.utils.ParameterSection
import com.example.gnssmap.utils.InfoRow
import com.example.gnssmap.app.theme.DeselectAllButtonColor
import com.example.gnssmap.app.theme.TextLabelColor
import com.example.gnssmap.app.theme.TextSecondaryColor
import com.example.gnssmap.app.theme.SelectAllButtonColor
import com.example.gnssmap.data.AzElHistory
import com.example.gnssmap.utils.Description
import com.example.gnssmap.utils.length
import com.example.gnssmap.utils.sub
import dev.romainguy.kotlin.math.Float3
import kotlin.math.cos
import kotlin.math.sin

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
            .background(DarkBackgroundColor.copy(alpha = 0.95f))
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
                        onClick = { navController.navigate("LocationMainScreen") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceColor)
                    ) {
                        Text("Location Info", color = TextLabelColor, fontSize = 14.sp)
                    }
                }

                Row(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { navController.navigate("SatelliteInfoMainScreen") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceColor)
                    ) {
                        Text("Satellite data", color = TextLabelColor, fontSize = 14.sp)
                    }
                }

                Text(
                    text = "Double tap on scene to open/close menu",
                    color = TextSecondaryColor,
                    fontSize = 14.sp
                )
                Text(
                    text = "Click on satellite to show info and approximated orbit if possible",
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
                    colors = ButtonDefaults.buttonColors(containerColor = SelectAllButtonColor)
                ) {
                    Text("Select All", color = TextLabelColor, fontSize = 12.sp)
                }
                Button(
                    onClick = { selectedConstellations.clear() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DeselectAllButtonColor)
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

            //val orbitEntries: Map<String, AzElHistory> = azElHistory.filterValues { hist ->
            //    (hist.firstAz != hist.lastAz) || (hist.firstEl != hist.lastEl)
            //}
            val orbitEntries by remember(azElHistory) {
                derivedStateOf {
                    azElHistory.filterValues { hist ->
                        azElDiffLen(hist.firstAz, hist.firstEl, hist.lastAz, hist.lastEl) > 0.032f
                    }
                }
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
        }
    }
}


private fun azElDiffLen(firstAz: Float, firstEl: Float, lastAz: Float, lastEl: Float): Float {
    fun toVec3(az: Float, el: Float): Float3 {
        val azRad = Math.toRadians(az.toDouble())
        val elRad = Math.toRadians(el.toDouble())
        val x = cos(elRad) * sin(azRad)
        val y = sin(elRad)
        val z = cos(elRad) * cos(azRad)
        return Float3(x.toFloat(), y.toFloat(), z.toFloat())
    }

    val firstPos = toVec3(firstAz, firstEl)
    val lastPos = toVec3(lastAz, lastEl)
    val diffLen = sub(firstPos, lastPos).length()
    Log.d("SatelliteManager", "check azElDiffLen ${diffLen}")
    return diffLen
}
