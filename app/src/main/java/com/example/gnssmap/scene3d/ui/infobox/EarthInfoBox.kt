package com.example.gnssmap.scene3d.ui.infobox

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.gnssmap.app.theme.CardBackgroundColor
import com.example.gnssmap.app.theme.TextPrimaryColor
import com.example.gnssmap.data.NMEALocationData
import com.example.gnssmap.data.parser.CoordinateConverter
import com.example.gnssmap.utils.InfoRow
import com.example.gnssmap.utils.mapFixQuality
import com.example.gnssmap.utils.mapFixType
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
        targetValue = if (isMenuVisible) totalMenuWidth
        else safeInsets.calculateLeftPadding(LayoutDirection.Ltr) * 2,
    )
    userLocation?.let { sat ->
        Box(
            modifier = modifier
                .padding(top = 20.dp)
                .offset(x = sceneOffsetX / 2)
        ) {
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .width(IntrinsicSize.Max),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackgroundColor.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Earth - Location Info",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimaryColor,
                        fontWeight = FontWeight.Bold,
                    )

                    val labelStyle = MaterialTheme.typography.bodySmall
                    val valueStyle = MaterialTheme.typography.bodyMedium

                    if (nmea.time.isNotEmpty()) {
                        InfoRow(
                            label = "Last update (UTC): ",
                            value = nmea.time,
                            labelStyle = labelStyle,
                            valueStyle = valueStyle
                        )
                    }

                    val fixInfo = listOf(
                        mapFixQuality(nmea.fixQuality),
                        mapFixType(nmea.fixType)
                    ).joinToString(" / ")

                    if (fixInfo.isNotBlank()) {
                        InfoRow(
                            label = "Fix / Type: ",
                            value = fixInfo,
                            labelStyle = labelStyle,
                            valueStyle = valueStyle
                        )
                    }

                    if (nmea.latitude != 0.0) {
                        InfoRow(
                            label = "Latitude: ",
                            value = CoordinateConverter.nmeaCoordinateToDMS(
                                nmea.latitude,
                                nmea.latHemisphere
                            ),
                            labelStyle = labelStyle,
                            valueStyle = valueStyle
                        )
                    }

                    if (nmea.longitude != 0.0) {
                        InfoRow(
                            label = "Longitude: ",
                            value = CoordinateConverter.nmeaCoordinateToDMS(
                                nmea.longitude,
                                nmea.lonHemisphere
                            ),
                            labelStyle = labelStyle,
                            valueStyle = valueStyle
                        )
                    }

                    if (nmea.mslAltitude != 0.0) {
                        InfoRow(
                            label = "Altitude MSL: ",
                            value = "%.1f m".format(nmea.mslAltitude),
                            labelStyle = labelStyle,
                            valueStyle = valueStyle
                        )
                    }

                    val accuracy = CoordinateConverter.getAccuracyEstimate(nmea)
                    if (accuracy > 0.0) {
                        InfoRow(
                            label = "Accuracy (2D): ",
                            value = "%.1f m".format(accuracy),
                            labelStyle = labelStyle,
                            valueStyle = valueStyle
                        )
                    }

                    if (nmea.speedKnots != 0.0) {
                        val speedKmh = nmea.speedKnots * 1.852
                        InfoRow(
                            label = "Speed: ",
                            value = listOf(
                                "%.1f kn".format(nmea.speedKnots),
                                "%.1f km/h".format(speedKmh)
                            ).joinToString(" / "),
                            labelStyle = labelStyle,
                            valueStyle = valueStyle
                        )
                    }

                }
            }
        }
    }
}
