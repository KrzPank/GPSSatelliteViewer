package com.example.gpssatelliteviewer.statisticscreen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.app.theme.DarkBackground

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun SNRStatisticsScreen(
    gnssViewModel: GNSSViewModel,
    nmeaMessageStatistics: Map<String, Int>
) {
    val snrStatistic by gnssViewModel.snrHistory.collectAsState()

    val keys = snrStatistic.keys.toList()

    LazyColumn(
        modifier = Modifier.Companion
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxSize()
            .background(DarkBackground),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val validKeys = keys.filter { key ->
            val snrList = snrStatistic[key]
            !snrList.isNullOrEmpty() && snrList.any { it != 0f }
        }

        if (validKeys.isEmpty()) {
            item {
                Text(
                    text = "No valid SNR data available",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            item{
            SNRChartCard(snrHistory = snrStatistic)
            }
            items(validKeys) { key ->
                SNRChartCard(
                    snrHistory = snrStatistic[key]!!,
                    constellation = key
                )
            }
        }
    }
}