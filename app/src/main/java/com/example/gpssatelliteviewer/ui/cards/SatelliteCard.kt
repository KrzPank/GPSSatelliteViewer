package com.example.gpssatelliteviewer.ui.cards


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
import com.example.gpssatelliteviewer.ui.components.InfoRow
import com.example.gpssatelliteviewer.data.GNSSData
import com.example.gpssatelliteviewer.ui.components.SNRBar


@Composable
fun SatelliteCard(
    satellites: List<GNSSData>,
    modifier: Modifier = Modifier
) {
    val totalSatellites = satellites.size

    val groupedSatellites = satellites.groupBy { it.constellation }
    val usedInFixCount = satellites.count { it.usedInFix }
    val averageSNRByConstellation = groupedSatellites.mapValues { entry ->
        val list = entry.value
        if (list.isNotEmpty()) list.map { it.snr }.average().toFloat() else 0f
    }


    val satellitesInFix = satellites.filter { it.usedInFix }
    val averageSNRInFix = if (satellitesInFix.isNotEmpty()) {
        satellitesInFix.map { it.snr }.average().toFloat()
    } else null

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow(label = "Widoczne satelity", value = totalSatellites.toString())
            InfoRow(label = "Używane w fix", value = usedInFixCount.toString())
            InfoRow(
                label = "Avg. fix SNR",
                value = if (averageSNRInFix != null) {
                    "${"%.1f".format(averageSNRInFix)} dBHz"
                } else {
                    "0"
                }
            )
            Spacer(modifier = modifier.height(6.dp))
            if (averageSNRInFix == null) SNRBar(value = 0f)
            else SNRBar(value = averageSNRInFix)

            //Spacer(modifier = Modifier.height(8.dp))
            Text("Średnie SNR:", style = MaterialTheme.typography.bodyMedium)

            averageSNRByConstellation.forEach { (constellation, avgSNR) ->
                InfoRow(
                    label = constellation,
                    value = "${"%.1f".format(avgSNR)} dBHz"
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

