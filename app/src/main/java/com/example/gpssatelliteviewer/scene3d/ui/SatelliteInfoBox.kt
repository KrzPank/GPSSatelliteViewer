package com.example.gpssatelliteviewer.scene3d.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.data.GNSSStatusData

@Composable
fun SatelliteInfoBox(
    liveClickedSatellite: GNSSStatusData?,
    modifier: Modifier = Modifier
) {
    liveClickedSatellite?.let { sat ->
        Box(
            modifier = modifier
                .padding(top = 50.dp)
        ) {
            Card(
                modifier = modifier.padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Column(modifier.padding(8.dp)) {
                    Text(
                        "${sat.constellation} PRN ${sat.prn}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text("SNR: ${sat.snr}", color = Color.White)
                    Text("Used in fix: ${sat.usedInFix}", color = Color.White)
                    Text(
                        "Azimuth: ${sat.azimuth}, Elevation: ${sat.elevation}",
                        color = Color.White
                    )
                }
            }
        }
    }
}