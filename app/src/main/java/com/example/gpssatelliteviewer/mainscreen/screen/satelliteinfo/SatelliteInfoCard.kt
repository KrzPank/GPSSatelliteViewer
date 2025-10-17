package com.example.gpssatelliteviewer.mainscreen.screen.satelliteinfo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.gpssatelliteviewer.data.GNSSCombinedData
import com.example.gpssatelliteviewer.data.GNSSStatusData
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
fun SatelliteInfoCard(satellite: GNSSCombinedData) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 1.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Always show constellation and SVID
            InfoRow("Constellation ", satellite.constellation)
            InfoRow("SVID/PRN ", "${satellite.svid ?:"N/A"} / ${satellite.prn}")
            InfoRow("SNR", "%.1f dBHz ".format(satellite.cn0DbHz))

            satellite.snrInDb?.let {
                InfoRow("SNR (dB) ", "%.1f dB".format(it))
            }

            InfoRow("Used in Fix ", satellite.usedInFix.toString())
            InfoRow("Azimuth ", "%.1f°".format(satellite.azimuth))
            InfoRow("Elevation ", "%.1f°".format(satellite.elevation))

            satellite.carrierFrequencyRangeHz?.let {
                InfoRow("Carrier Frequency ", "%.1f Hz".format(it))
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
        }
    }
}
