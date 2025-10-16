package com.example.gpssatelliteviewer.scene3d.ui.infobox

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.app.theme.CardBackground
import com.example.gpssatelliteviewer.data.GNSSStatusData

@Composable
fun SatelliteInfoBox(
    clickedSatellite: GNSSStatusData?,
    isMenuVisible: Boolean,
    safeInsets: PaddingValues,
    totalMenuWidth: Dp,
    modifier: Modifier = Modifier
) {
    val sceneOffsetX by animateDpAsState(
        targetValue = if (isMenuVisible) totalMenuWidth else safeInsets.calculateLeftPadding(LayoutDirection.Ltr) * 2,
    )
    clickedSatellite?.let { sat ->
        Box(
            modifier = modifier
                .padding(top = 50.dp)
                .offset(x = sceneOffsetX / 2)
        ) {
            Card(
                modifier = modifier.padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground.copy(alpha = 0.4f)
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
                    Text("Azimuth: ${sat.azimuth}, Elevation: ${sat.elevation}", color = Color.White)
                }
            }
        }
    }
}