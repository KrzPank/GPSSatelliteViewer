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
import androidx.compose.material3.MaterialTheme
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
import com.example.gpssatelliteviewer.data.GNSSCombinedData
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.utils.InfoRow

@Composable
fun SatelliteInfoBox(
    clickedSatellite: GNSSCombinedData?,
    isMenuVisible: Boolean,
    safeInsets: PaddingValues,
    totalMenuWidth: Dp,
    modifier: Modifier = Modifier
) {
    val sceneOffsetX by animateDpAsState(
        targetValue = if (isMenuVisible) totalMenuWidth / 2 else safeInsets.calculateLeftPadding(LayoutDirection.Ltr),
    )
    clickedSatellite?.let { satellite ->
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
                        text = satellite.constellation,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    val labelStyle = MaterialTheme.typography.bodySmall
                    val valueStyle = MaterialTheme.typography.bodyMedium

                    InfoRow("Constellation", satellite.constellation, labelStyle = labelStyle, valueStyle = valueStyle)
                    InfoRow("SVID/PRN", "${satellite.svid ?: "N/A"} / ${satellite.prn}", labelStyle = labelStyle, valueStyle = valueStyle)
                    InfoRow("SNR", "%.1f dBHz".format(satellite.cn0DbHz), labelStyle = labelStyle, valueStyle = valueStyle)

                    satellite.snrInDb?.let {
                        InfoRow("SNR (dB)", "%.1f dB".format(it), labelStyle = labelStyle, valueStyle = valueStyle)
                    }

                    InfoRow("Used in Fix", satellite.usedInFix.toString(), labelStyle = labelStyle, valueStyle = valueStyle)
                    InfoRow("Azimuth", "%.1f°".format(satellite.azimuth), labelStyle = labelStyle, valueStyle = valueStyle)
                    InfoRow("Elevation", "%.1f°".format(satellite.elevation), labelStyle = labelStyle, valueStyle = valueStyle)

                    satellite.carrierFrequencyRangeHz?.let {
                        InfoRow("Carrier Frequency", "%.1f Hz".format(it), labelStyle = labelStyle, valueStyle = valueStyle)
                    }

                    satellite.accumulatedDeltaRangeMeters?.let {
                        InfoRow("Accum. Δ Range", "%.3f m".format(it), labelStyle = labelStyle, valueStyle = valueStyle)
                    }

                    satellite.accumulatedDeltaRangeUncertaintyMeters?.let {
                        InfoRow("Accum. Δ Range Uncer.", "%.3f m".format(it), labelStyle = labelStyle, valueStyle = valueStyle)
                    }

                    satellite.pseudorangeRateMetersPerSecond?.let {
                        InfoRow("Pseudorange", "%.3f m/s".format(it), labelStyle = labelStyle, valueStyle = valueStyle)
                    }

                    satellite.pseudorangeRateUncertaintyMetersPerSecond?.let {
                        InfoRow("Pseudorange Rate Uncer.", "%.3f m/s".format(it), labelStyle = labelStyle, valueStyle = valueStyle)
                    }

                    satellite.timeOffsetNanos?.let {
                        InfoRow("Time Offset", "%.6f ns".format(it), labelStyle = labelStyle, valueStyle = valueStyle)
                    }
                }
            }
        }
    }
}
