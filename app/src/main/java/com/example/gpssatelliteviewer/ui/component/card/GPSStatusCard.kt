package com.example.gpssatelliteviewer.ui.component.card


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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpssatelliteviewer.utils.InfoRow
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.ui.theme.TextLabel
import com.example.gpssatelliteviewer.ui.theme.ValueText
import com.example.gpssatelliteviewer.utils.GPSStatus


@Composable
fun GPSStatusCard(
    satellites: List<GNSSStatusData>,
    hasLocation: Boolean,
    modifier: Modifier = Modifier
) {
    val gpsStatus = GPSStatus(satellites, hasLocation)

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
            gpsStatus.GPSStatusHeader()
            
            Spacer(modifier = Modifier.height(16.dp))

            // Existing satellite info
            InfoRow(label = "In view", value = gpsStatus.getSatelliteCount().toString())
            InfoRow(label = "Used in fix", value = gpsStatus.getFixCount().toString())
            InfoRow(
                label = "Avg. fix SNR",
                value = "${"%.1f".format(gpsStatus.averageSNRInFix)} dBHz"
            )

            Spacer(modifier = Modifier.height(8.dp))
            gpsStatus.SNRBar()

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Constellation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLabel,
                    fontSize = 15.sp
                )
                Text(
                    text = "Avg. SNR/in Fix",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLabel,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            gpsStatus.averageSNRByConstellation.forEach { (constellation, snr) ->
                InfoRow(
                    label = constellation,
                    value = "${"%.1f".format(snr)} dBHz / ${gpsStatus.getFixCountByConstellation(constellation)}"
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
