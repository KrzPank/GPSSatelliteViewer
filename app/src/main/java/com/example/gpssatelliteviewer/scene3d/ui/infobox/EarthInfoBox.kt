package com.example.gpssatelliteviewer.scene3d.ui.infobox

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpssatelliteviewer.app.theme.CardBackground
import com.example.gpssatelliteviewer.data.NMEALocationData
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import com.example.gpssatelliteviewer.utils.InfoRow
import com.example.gpssatelliteviewer.utils.mapFixQuality
import com.example.gpssatelliteviewer.utils.mapFixType
import dev.romainguy.kotlin.math.Float3

@Composable
fun EarthInfoBox(
    userLocation: Float3?,
    nmea: NMEALocationData,
    isMenuVisible: Boolean,
    safeInsets: PaddingValues,
    totalMenuWidth: Dp,
    modifier: Modifier = Modifier
) {
    val sceneOffsetX by animateDpAsState(
        targetValue = if (isMenuVisible) totalMenuWidth else safeInsets.calculateLeftPadding(
            LayoutDirection.Ltr
        ) * 2,
    )
    userLocation?.let { sat ->
        Box(
            modifier = modifier
                .padding(top = 50.dp)
                .offset(x = sceneOffsetX / 2)
        ) {
            Card(
                modifier = modifier.padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground.copy(alpha = 0.6f)
                )
            ) {
                Column(Modifier.padding(12.dp)) {

                    if (nmea.time.isNotEmpty()) {
                        Text(
                            text = "Last update (UTC): " + nmea.time,
                            fontSize = 13.sp
                        )
                    }

                    val fixInfo = listOf(
                        mapFixQuality(nmea.fixQuality),
                        mapFixType(nmea.fixType)
                    ).joinToString(" / ")
                    if (fixInfo.isNotBlank()) {
                        Text(
                            text = "Fix / Type: $fixInfo",
                            fontSize = 13.sp
                        )
                    }

                    if (nmea.latitude != 0.0) {
                        Text(
                            text = "Latitude: " + CoordinateConverter.nmeaCoordinateToDMS(nmea.latitude, nmea.latHemisphere),
                            fontSize = 13.sp
                        )
                    }

                    if (nmea.longitude != 0.0) {
                        Text(
                            text = "Longitude: " + CoordinateConverter.nmeaCoordinateToDMS(nmea.longitude, nmea.lonHemisphere),
                            fontSize = 13.sp
                        )
                    }

                    if (nmea.mslAltitude != 0.0) {
                        Text(
                            text = "Altitude MSL: " + "%.1f m".format(nmea.mslAltitude),
                            fontSize = 13.sp
                        )
                    }

                    val accuracy = CoordinateConverter.getAccuracyEstimate(nmea)
                    if (accuracy > 0.0) {
                        Text(
                            text = "Accuracy (2D): " + "%.1f m".format(accuracy),
                            fontSize = 13.sp
                        )
                    }

                    if (nmea.speedKnots != 0.0) {
                        val speedkmh = nmea.speedKnots * 1.852
                        Text(
                            text = "Speed: " + listOf(
                                "%.1f kn".format(nmea.speedKnots),
                                "%.1f km/h".format(speedkmh)
                            ).joinToString(" / "),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}