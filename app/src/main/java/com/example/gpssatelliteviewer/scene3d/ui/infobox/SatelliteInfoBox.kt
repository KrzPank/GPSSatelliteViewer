package com.example.gpssatelliteviewer.scene3d.ui.infobox

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.gpssatelliteviewer.utils.InfoRow

@Composable
fun SatelliteInfoBox(
    clickedSatellite: GNSSStatusData?,
    isMenuVisible: Boolean,
    safeInsets: PaddingValues,
    totalMenuWidth: Dp,
    modifier: Modifier = Modifier
) {
    val sceneOffsetX by animateDpAsState(
        targetValue = if (isMenuVisible) totalMenuWidth / 2 else safeInsets.calculateLeftPadding(LayoutDirection.Ltr),
    )
    clickedSatellite?.let { sat ->
        Box(
            modifier = modifier
                .padding(top = 20.dp)
                .offset(x = sceneOffsetX)
        ) {
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .width(IntrinsicSize.Max),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${sat.constellation} PRN ${sat.prn}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    InfoRow(label = "SNR: ", value = sat.cn0DbHz.toString())
                    InfoRow(label = "In fix: ", value = sat.usedInFix.toString())
                    InfoRow(label = "Azimuth: ", value = sat.azimuth.toString())
                    InfoRow(label = "Elevation: ", value = sat.elevation.toString())
                }
            }
        }
    }
}
