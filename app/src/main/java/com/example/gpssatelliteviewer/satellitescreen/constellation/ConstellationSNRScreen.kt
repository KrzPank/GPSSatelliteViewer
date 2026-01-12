package com.example.gpssatelliteviewer.satellitescreen.constellation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WrongLocation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import com.example.gpssatelliteviewer.utils.EmptyStateCard
import kotlin.collections.any
import kotlin.collections.isNullOrEmpty
import androidx.compose.runtime.getValue

@Composable
fun ConstellationSNRScreen(
    gnssViewModel: GNSSViewModel
) {
    val snrConstellationHistory by gnssViewModel.constellationSNRHistory.collectAsState()
    val satCountByConstellation by gnssViewModel.satelliteCountHistory.collectAsState()

    val constellation = snrConstellationHistory.keys.toList()

    val validConstellation = constellation.filter { key ->
        val snrList = snrConstellationHistory[key]
        !snrList.isNullOrEmpty() && snrList.any { it.snr != 0f }
    }

    LazyColumn(
        modifier = Modifier.Companion
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (validConstellation.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "No valid data available",
                    icon = Icons.Default.WrongLocation
                )
            }
        } else {
            item {

                Spacer(Modifier.height(6.dp))
                GroupedConstellationSNRChartCard(
                    snrHistory = snrConstellationHistory,
                    label = "Avg. C/N0 for constellations in fix",
                    modifier = Modifier.Companion.height(280.dp)
                )
            }

            item {
                Spacer(Modifier.height(15.dp))
                HorizontalDivider()

                GroupedConstellationSNRChartCard(
                    snrHistory = satCountByConstellation,
                    label = "Satellites used in fix count by constellation in time",
                    modifier = Modifier.Companion.height(280.dp)
                )
            }

            //items(validConstellation) { constellation ->
            //    IndividualConstellationSNRChartCard(
            //        constellationSNRHistory = snrConstellationHistory[constellation]!!,
            //        constellation = constellation,
            //        modifier = Modifier.Companion.height(240.dp)
            //    )
            //}
        }
    }
}
