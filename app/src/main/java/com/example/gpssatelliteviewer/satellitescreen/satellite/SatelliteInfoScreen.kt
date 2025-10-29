package com.example.gpssatelliteviewer.satellitescreen.satellite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.gpssatelliteviewer.data.mergeLists
import com.example.gpssatelliteviewer.satellitescreen.satellite.ConstellationCard
import com.example.gpssatelliteviewer.satellitescreen.satellite.FilterDialog
import com.example.gpssatelliteviewer.satellitescreen.satellite.FilterOptionsCard
import com.example.gpssatelliteviewer.satellitescreen.satellite.SatelliteInfoCard
import com.example.gpssatelliteviewer.utils.EmptyStateCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteInfoScreen(
    gnssViewModel: GNSSViewModel
) {
    val satellites by gnssViewModel.satelliteList.collectAsState()
    val measurements by gnssViewModel.gnssMeasurements.collectAsState()
    val satelliteInfo = mergeLists(satellites, measurements)

    val satelliteSNRHistory by gnssViewModel.satelliteSNRHistory.collectAsState()

    var showOnlyInFix by remember { mutableStateOf(false) }
    var selectedConstellations by remember { mutableStateOf(setOf<String>()) }
    var selectedSatellites by remember { mutableStateOf(setOf<String>()) }

    val filteredSatellites = satelliteInfo
        .filter { sat ->
            (!showOnlyInFix || sat.usedInFix) &&
                    (selectedConstellations.isEmpty() || selectedConstellations.contains(sat.constellation)) &&
                    (selectedSatellites.isEmpty() || selectedSatellites.contains("${sat.constellation}:${sat.prn}"))
        }
        .sortedBy { it.prn }
        .groupBy { it.constellation }

    var showFilterDialog by remember { mutableStateOf(false) }

    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier.Companion
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            FilterOptionsCard(
                showFilterDialog = showFilterDialog,
                onToggle = { showFilterDialog = it }
            )
        }

        item { /* empty item as 4.dp spacer */ }

        if (filteredSatellites.isNotEmpty()) {
            filteredSatellites.forEach { (constellation, satellitesInGroup) ->

                val expanded = expandedMap.getOrPut(constellation) { false }

                item {
                    ConstellationCard(
                        constellation = constellation,
                        satellitesCount = satellitesInGroup.size,
                        expanded = expanded,
                        onClick = { expandedMap[constellation] = !expanded }
                    )
                }

                if (expanded) {
                    items(satellitesInGroup) { satellite ->
                        val key = "${satellite.constellation}:${satellite.prn}"
                        SatelliteInfoCard(
                            satellite = satellite,
                            snrHistory = satelliteSNRHistory.filter { it.key == key && it.value.any() { it.snr != 0f} },
                            modifier = Modifier.Companion.height(150.dp)
                        )
                    }
                }
            }
        } else {
            item {
                val message = if (showOnlyInFix) "No valid satellite information."
                else "No satellite information received yet."

                EmptyStateCard(
                    message = message,
                    icon = Icons.Default.SatelliteAlt
                )
            }
        }
    }

    // --- FILTER DIALOG ---
    if (showFilterDialog) {
        FilterDialog(
            showOnlyInFix = showOnlyInFix,
            onShowOnlyInFixChange = { showOnlyInFix = it },
            satellites =  satellites,
            selectedConstellations = selectedConstellations,
            onConstellationsChange = { selectedConstellations = it },
            selectedSatellites = selectedSatellites,
            onSatellitesChange = { selectedSatellites = it },
            onDismiss = { showFilterDialog = false }
        )
    }
}
