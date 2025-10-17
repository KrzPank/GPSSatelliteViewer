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
import com.example.gpssatelliteviewer.utils.ValueText

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
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.Companion.padding(16.dp)) {
            Text(
                "GPS Status",
                style = MaterialTheme.typography.headlineMedium,
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
                color = TextLabel
            )
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Fix SNR dBHz / sats  |  All dBHz / sats SNR!=0 / all",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextLabel,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))

                gpsStatus.averageSNRByConstellation.forEach { (constellation, avgAll) ->
                    val avgFix = gpsStatus.averageSNRByConstellationInFix[constellation] ?: 0.0f
                    val fixCount = gpsStatus.getFixCountByConstellation(constellation)
                    val totalSNRCount = gpsStatus.getSNRNot0CountByConstellation(constellation)
                    val totalSatellites = gpsStatus.getGroupedSatelliteCount(constellation)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = constellation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextLabel,
                            modifier = Modifier.weight(0.8f)
                        )
                        ValueText(
                            value = "%.1f / %d".format(avgFix, fixCount),
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.7f)
                        )
                        ValueText(
                            value = "  |  ",
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.2f)
                        )
                        ValueText(
                            value = "%.1f / %d / %d".format(avgAll, totalSNRCount, totalSatellites),
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}