package com.example.gpssatelliteviewer.ui.component.card


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpssatelliteviewer.utils.InfoRow
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.utils.GPSStatusUtils


@Composable
fun GPSStatusCard(
    satellites: List<GNSSStatusData>,
    modifier: Modifier = Modifier
) {
    val totalSatellitesCount = GPSStatusUtils.getTotalSatelliteCount(satellites)
    val usedInFixCount = GPSStatusUtils.getUsedInFixCount(satellites)
    val averageSNRByConstellation = GPSStatusUtils.calculateAverageSNRByConstellation(satellites)
    val averageSNRInFix = GPSStatusUtils.calculateAverageSNRInFix(satellites)

    // NEW: Calculate GPS status
    val gpsStatus = GPSStatusUtils.determineGPSStatus(
        satellites = satellites,
        averageSNRInFix,
        hasLocationNMEA = satellites.isNotEmpty()
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "GPS Status",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(8.dp))
            
            // GPS Status Header
            GPSStatusUtils.GPSStatusHeader(gpsStatus = gpsStatus)
            
            Spacer(modifier = Modifier.height(16.dp))

            // Existing satellite info
            InfoRow(label = "In view", value = totalSatellitesCount.toString())
            InfoRow(label = "Used in fix", value = usedInFixCount.toString())
            InfoRow(
                label = "Avg. fix SNR",
                value = if (averageSNRInFix != 0f) "${"%.1f".format(averageSNRInFix)} dBHz"
                else  "0"
            )

            Spacer(modifier = Modifier.height(8.dp))
            GPSStatusUtils.SNRBar(value = averageSNRInFix)

            Spacer(modifier = Modifier.height(4.dp))
            Text("Constellation  Avg. SNR/sat SNR!=0", style = MaterialTheme.typography.bodyMedium)

            averageSNRByConstellation.forEach { (constellation, stats) ->
                InfoRow(
                    label = constellation,
                    value = "${"%.1f".format(stats.average)} dBHz / ${"%.0f".format(stats.count)}"
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
