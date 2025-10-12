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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpssatelliteviewer.app.theme.TextLabel
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

            Spacer(modifier = Modifier.Companion.height(4.dp))
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Average SNR for satellites in Fix per constellation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLabel,
                    fontSize = 15.sp
                )
                //Text(
                //    text = "Avg. SNR/In fix",
                //    style = MaterialTheme.typography.bodyMedium,
                //    color = TextLabel,
                //    fontSize = 15.sp
                //)
            }

            Spacer(modifier = Modifier.Companion.height(4.dp))
            gpsStatus.averageSNRByConstellationInFix.forEach { (constellation, snr) ->
                InfoRow(
                    label = constellation,
                    value = "${"%.1f".format(snr)} dBHz / ${
                        gpsStatus.getFixCountByConstellation(
                            constellation
                        )
                    }"
                )
                Spacer(Modifier.Companion.height(4.dp))
            }

            Spacer(Modifier.Companion.height(4.dp))
            Text(
                text = "Average SNR per constellation",
                style = MaterialTheme.typography.bodyMedium,
                color = TextLabel,
                fontSize = 15.sp
            )
            gpsStatus.averageSNRByConstellation.forEach { (constellation, snr) ->
                InfoRow(
                    label = constellation,
                    value = "${"%.1f".format(snr)} dBHz / ${
                        gpsStatus.getFixCountByConstellation(
                            constellation
                        )
                    }"
                )
                Spacer(Modifier.Companion.height(4.dp))
            }
        }
    }
}