package com.example.gpssatelliteviewer.statisticscreen.constellationSNRstatistic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WrongLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import com.example.gpssatelliteviewer.statisticscreen.IndividualSNRChart
import com.example.gpssatelliteviewer.utils.EmptyStateCard
import kotlin.collections.any
import kotlin.collections.isNullOrEmpty
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp

@Composable
fun ConstellationSNRStatisticsScreen(
    gnssViewModel: GNSSViewModel
) {
    val snrStatistic by gnssViewModel.constellationSNRHistory.collectAsState()
    val constellation = snrStatistic.keys.toList()

    val validConstellation = constellation.filter { key ->
        val snrList = snrStatistic[key]
        !snrList.isNullOrEmpty() && snrList.any { it != 0f }
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
                    message = "No valid SNR data available",
                    icon = Icons.Default.WrongLocation
                )
            }
        } else {
            item {
                Text(
                    text = "Avg. SNR for all constellations in fix",
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(6.dp))
                GroupedConstellationSNRChartCard(
                    snrHistory = snrStatistic,
                    modifier = Modifier.Companion.height(240.dp)
                )
            }

            item {
                Text(
                    text = "Avg. SNR for individual constellations in fix",
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 20.sp
                )
            }
            items(validConstellation) { constellation ->
                IndividualConstellationSNRChartCard(
                    constellationSNRHistory = snrStatistic[constellation]!!,
                    constellation = constellation,
                    modifier = Modifier.Companion.height(200.dp)
                )
            }
        }
    }
}
