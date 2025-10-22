package com.example.gpssatelliteviewer.statisticscreen.satelliteSNRstatistic

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.statisticscreen.IndividualSNRChart
import com.example.gpssatelliteviewer.statisticscreen.getConstellationColor

@Composable
fun SatelliteSNRChartCard(
    satelliteSNRHistory: List<Float>,
    key: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.Companion
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        val label = key.split(":")
        IndividualSNRChart(
            snrHistory = satelliteSNRHistory,
            lineColor = getConstellationColor(label.first()),
            label = "${label.first()} ${label.last()}",
            modifier = modifier
        )
    }
}
