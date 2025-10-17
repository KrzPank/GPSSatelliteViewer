package com.example.gpssatelliteviewer.statisticscreen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.SignalCellularNodata
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.app.theme.DarkBackground
import com.example.gpssatelliteviewer.utils.EmptyStateCard

@Composable
fun SNRStatisticsScreen(
    gnssViewModel: GNSSViewModel
) {
    val snrStatistic by gnssViewModel.constellationSNRHistory.collectAsState()
    val keys = snrStatistic.keys.toList()

    LazyColumn(
        modifier = Modifier.Companion
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxSize()
            .background(DarkBackground),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val validKeys = keys.filter { key ->
            val snrList = snrStatistic[key]
            !snrList.isNullOrEmpty() && snrList.any { it != 0f }
        }

        if (validKeys.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "No valid SNR data available",
                    icon = Icons.Default.LocationOff
                )
            }
        } else {
            item {
                ConstellationSNRChartCard(
                    snrHistory = snrStatistic,
                    modifier = Modifier.height(220.dp)
                )
            }

            items(validKeys) { key ->
                ConstellationSNRChartCard(
                    snrHistory = snrStatistic[key]!!,
                    constellation = key,
                    modifier = Modifier.height(190.dp)
                )
            }
        }
    }
}