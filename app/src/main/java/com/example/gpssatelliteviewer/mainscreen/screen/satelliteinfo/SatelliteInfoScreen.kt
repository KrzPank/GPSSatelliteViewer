package com.example.gpssatelliteviewer.mainscreen.screen.satelliteinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import com.example.gpssatelliteviewer.app.theme.GreenPrimaryColor
import com.example.gpssatelliteviewer.app.theme.TextPrimaryColor
import com.example.gpssatelliteviewer.data.mergeLists
import com.example.gpssatelliteviewer.utils.EmptyStateCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteInfoScreen(
    gnssViewModel: GNSSViewModel
) {
    val satellites by gnssViewModel.satelliteList.collectAsState()
    val measurements by gnssViewModel.gnssMeasurements.collectAsState()
    val satelliteInfo = mergeLists(satellites, measurements)

    var showOnlyInfFix by remember { mutableStateOf(false) }

    val filteredSatellites = satelliteInfo.let { list ->
        if (showOnlyInfFix) list.filter { it.usedInFix }
        else list
    }
        .sortedBy { it.prn }
        .groupBy { it.constellation }

    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier.Companion
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Checkbox(
                    checked = showOnlyInfFix,
                    onCheckedChange = { showOnlyInfFix = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = GreenPrimaryColor,
                        uncheckedColor = TextPrimaryColor
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Show only used in Fix",
                    color = TextPrimaryColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

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
                        SatelliteInfoCard(satellite)
                    }
                }

                item { /* empty item as 4.dp spacer */ }
            }
        } else {
            item {
                val message = if (showOnlyInfFix) "No valid satellite information."
                else "No satellite information received yet."

                EmptyStateCard(
                    message = message,
                    icon = Icons.Default.SatelliteAlt
                )
            }
        }
    }
}
