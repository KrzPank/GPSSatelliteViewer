package com.example.gpssatelliteviewer.satellitescreen.satellite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.app.theme.TextLabelColor
import com.example.gpssatelliteviewer.data.GNSSCombinedData
import com.example.gpssatelliteviewer.data.TimestampedSNR
import com.example.gpssatelliteviewer.satellitescreen.IndividualSNRChart
import com.example.gpssatelliteviewer.satellitescreen.getConstellationColor
import com.example.gpssatelliteviewer.utils.InfoRow

@Composable
fun ConstellationCard(
    constellation: String,
    satellitesCount: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.Companion
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Text(
                text = "$constellation ($satellitesCount)",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Companion.Medium
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown
                else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null
            )
        }
    }
}

@Composable
fun SatelliteInfoCard(
    satellite: GNSSCombinedData,
    snrHistory: Map<String, List<TimestampedSNR>>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 1.dp)
    ) {
        Column(modifier = Modifier.Companion.padding(12.dp)) {

            InfoRow("Constellation ", satellite.constellation)
            InfoRow("SVID/PRN ", "${satellite.svid ?: "N/A"} / ${satellite.prn}")
            InfoRow("C/N0", "%.1f dBHz ".format(satellite.cn0DbHz))

            satellite.snrInDb?.let {
                InfoRow("SNR ", "%.1f dB".format(it))
            }

            InfoRow("Used in Fix ", satellite.usedInFix.toString())
            InfoRow("Azimuth ", "%.1f°".format(satellite.azimuth))
            InfoRow("Elevation ", "%.1f°".format(satellite.elevation))

            satellite.carrierFrequencyRangeHz?.let {
                InfoRow("Carrier Frequency ", "%.3f MHz".format(it / 1_000_000))
            }

            satellite.accumulatedDeltaRangeMeters?.let {
                InfoRow("Accum. Δ Range ", "%.3f m".format(it))
            }

            satellite.accumulatedDeltaRangeUncertaintyMeters?.let {
                InfoRow("Accum. Δ Range Uncer. ", "%.3f m".format(it))
            }

            satellite.pseudorangeRateMetersPerSecond?.let {
                InfoRow("Pseudorange  ", "%.3f m/s".format(it))
            }

            satellite.pseudorangeRateUncertaintyMetersPerSecond?.let {
                InfoRow("Pseudorange Rate Uncer. ", "%.3f m/s".format(it))
            }

            satellite.timeOffsetNanos?.let {
                InfoRow("Time Offset ", "%.6f ns".format(it))
            }

            if (snrHistory.isNotEmpty()) {
                val key = snrHistory.keys.first()
                Spacer(Modifier.Companion.height(8.dp))
                Text(
                    text = "C/N0 dBHz in time",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextLabelColor
                )
                IndividualSNRChart(
                    snrHistory = snrHistory[key]!!,
                    lineColor = getConstellationColor(key.substringBefore(":").trim()),
                    modifier = modifier.height(150.dp),
                )
            }
        }
    }
}
