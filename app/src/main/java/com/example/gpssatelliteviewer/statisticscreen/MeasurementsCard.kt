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

            if (measurement.snrInDb != null) {
                Text(
                    text = "SNR: %.1f dB".format(measurement.snrInDb),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }

            measurement.accumulatedDeltaRangeMeters?.let {
                Text(
                    text = "Carrier Phase Δ: %.3f m".format(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }

            measurement.pseudorangeRateMetersPerSecond?.let {
                Text(
                    text = "Doppler Rate: %.3f m/s".format(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }

            measurement.accumulatedDeltaRangeUncertaintyMeters?.let {
                Text(
                    text = "Uncertainty: %.3f m".format(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }

            measurement.timeOffsetNanos?.let {
                Text(
                    text = "Clock Drift: %.3f ns".format(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }
    }
}
