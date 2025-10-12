package com.example.gpssatelliteviewer.statisticscreen

import android.annotation.SuppressLint
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.data.GNSSMeasurementData
import com.example.gpssatelliteviewer.utils.InfoRow

@SuppressLint("DefaultLocale")
@Composable
fun MeasurementCard(measurement: GNSSMeasurementData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${measurement.constellation}  #${measurement.svid}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = String.format("%.1f dB-Hz", measurement.cn0DbHz),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF80CBC4)
                )
            }

            Spacer(Modifier.height(6.dp))

            Spacer(Modifier.height(8.dp))

            // Information Rows
            measurement.snrInDb?.let {
                InfoRow(label = "SNR", value = String.format("%.1f dB", it))
            }

            measurement.carrierFrequencyRangeHz?.let {
                InfoRow(label = "Carrier Freq", value = String.format("%.3f MHz", it / 1_000_000.0))
            }

            measurement.accumulatedDeltaRangeMeters?.let {
                InfoRow(label = "Δ Range", value = String.format("%.3f m", it))
            }

            measurement.accumulatedDeltaRangeUncertaintyMeters?.let {
                InfoRow(label = "Δ Range Unc.", value = String.format("%.3f m", it))
            }

            measurement.pseudorangeRateMetersPerSecond?.let {
                InfoRow(label = "Pseudorange Rate", value = String.format("%.3f m/s", it))
            }

            measurement.pseudorangeRateUncertaintyMetersPerSecond?.let {
                InfoRow(label = "Rate Uncertainty", value = String.format("%.3f m/s", it))
            }

            measurement.timeOffsetNanos?.let {
                InfoRow(label = "Clock Bias", value = String.format("%.3f ns", it))
            }
        }
    }
}
