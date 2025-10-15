package com.example.gpssatelliteviewer.mainscreen.screen.locationinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpssatelliteviewer.app.theme.TextLabel
import com.example.gpssatelliteviewer.app.theme.ValueText
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.utils.InfoRow

@Composable
fun GPSStatusCard(
    satellites: List<GNSSStatusData>,
    hasLocation: Boolean,
    isLocationEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val gpsStatus = GPSStatus(satellites, hasLocation, isLocationEnabled)

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.Companion.padding(16.dp)) {
            Text(
                "GPS Status",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 20.sp
            )
            Spacer(Modifier.Companion.height(8.dp))

            // GPS Status Header
            gpsStatus.GPSStatusHeader()

            Spacer(modifier = Modifier.Companion.height(16.dp))

            // Existing satellite info
            InfoRow(label = "In view", value = gpsStatus.getSatelliteCount().toString())
            InfoRow(label = "Used in fix", value = gpsStatus.getFixCount().toString())
            InfoRow(
                label = "Avg. fix SNR",
                value = "${"%.1f".format(gpsStatus.averageSNRInFix)} dBHz"
            )

            Spacer(modifier = Modifier.Companion.height(8.dp))
            gpsStatus.SNRBar()

            Spacer(Modifier.height(8.dp))
            Text(
                text = "SNR Summary per Constellation",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                color = TextLabel
            )
            Text(
                text = "Fix SNR / sats | All SNR!=0 / sats SNR!=0 / all",
                style = MaterialTheme.typography.bodySmall,
                color = TextLabel,
                fontSize = 13.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))

            gpsStatus.averageSNRByConstellation.forEach { (constellation, avgAll) ->
                val avgFix = gpsStatus.averageSNRByConstellationInFix[constellation] ?: 0.0f
                val fixCount = gpsStatus.getFixCountByConstellation(constellation)
                val totalSNRCount = gpsStatus.getSNRNot0CountByConstellation(constellation)
                val totalSatellites = gpsStatus.getGroupedSatelliteCount(constellation)

                InfoRow(
                    label = constellation,
                    value = "Fix: %.1f dBHz / %d  |  All: %.1f dBHz / %d / %d".format(
                        avgFix, fixCount, avgAll, totalSNRCount, totalSatellites
                    )
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}