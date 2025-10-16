package com.example.gpssatelliteviewer.mainscreen.screen.satelliteinfo

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
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
import com.example.gpssatelliteviewer.app.theme.DarkBackground
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import com.example.gpssatelliteviewer.mainscreen.screen.satelliteinfo.ConstellationCard
import com.example.gpssatelliteviewer.mainscreen.screen.satelliteinfo.SatelliteInfoCard
import androidx.compose.runtime.getValue
import com.example.gpssatelliteviewer.utils.EmptyStateCard

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun SatelliteInfoScreen(
    gnssViewModel: GNSSViewModel
) {

    // merge measurements to satellite info card and make filter button to show only those with fix == true

    val satellites by gnssViewModel.satelliteList.collectAsState()

    // add button to sort by fix
    val groupedSatellites = satellites
        .sortedBy { it.prn }
        .groupBy { it.constellation }

    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier.Companion
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxSize()
            .background(DarkBackground),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (groupedSatellites.isNotEmpty()) {
            groupedSatellites.forEach { (constellation, satellitesInGroup) ->
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
                    items(satellitesInGroup) { satellites ->
                        SatelliteInfoCard(satellites)
                    }
                }

                item { /* empty item as 4.dp spacer */ }
            }
        } else {
            item {
                EmptyStateCard(
                    message = "No satellite information received yet.",
                    icon = Icons.Default.SatelliteAlt
                )
            }
        }
    }
}