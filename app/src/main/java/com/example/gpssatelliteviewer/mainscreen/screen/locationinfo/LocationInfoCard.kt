package com.example.gpssatelliteviewer.mainscreen.screen.locationinfo

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpssatelliteviewer.data.NMEALocationData
import com.example.gpssatelliteviewer.data.viewmodel.LocationViewModel.ListenerData
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import com.example.gpssatelliteviewer.utils.InfoRow
import com.example.gpssatelliteviewer.utils.mapFixQuality
import com.example.gpssatelliteviewer.utils.mapFixType

@Composable
fun NMEALocationCard(
    nmea: NMEALocationData,
    currentSystemTime: String,
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.Companion.padding(12.dp)) {
            Row(
                modifier = Modifier.Companion
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "NMEA Location",
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 20.sp
                )
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Location Settings",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(enabled = onSettingsClick != null) {
                            onSettingsClick?.invoke()
                        }
                )
            }

            Spacer(Modifier.Companion.height(8.dp))

            InfoRow(
                label = "Current system time",
                value = currentSystemTime
            )
            InfoRow(
                label = "Last update (UTC)",
                value = if (nmea.time == "") "N/A"
                else nmea.time
            )
            InfoRow(
                label = "Fix / Type",
                value = listOf(
                    mapFixQuality(nmea.fixQuality),
                    mapFixType(nmea.fixType)
                ).joinToString(" / ")
            )
            InfoRow(
                label = "Date",
                value = if (nmea.date == "") "N/A"
                else nmea.date
            )
            InfoRow(
                label = "Latitude",
                value = if (nmea.latitude == 0.0) "N/A"
                else CoordinateConverter.nmeaCoordinateToDMS(nmea.latitude, nmea.latHemisphere)
            )
            InfoRow(
                label = "Longitude",
                value = if (nmea.longitude == 0.0) "N/A"
                else CoordinateConverter.nmeaCoordinateToDMS(nmea.longitude, nmea.lonHemisphere)
            )
            InfoRow(
                label = "Accuracy (2D)",
                value = CoordinateConverter.getAccuracyEstimate(nmea).let { "%.1f m".format(it) }
            )
            InfoRow(
                label = "H / V / PDOP",
                value = listOf(
                    nmea.hdop.let { "%.1f m".format(it) },
                    nmea.vdop.let { "%.1f m".format(it) },
                    nmea.pdop.let { "%.1f m".format(it) }
                ).joinToString(" / ")
            )
            InfoRow(
                label = "Alt / Alt MSL",
                value = listOf(
                    nmea.altitude.let { "%.1f m".format(it) },
                    nmea.mslAltitude.let { "%.1f m".format(it) }
                ).joinToString(" / ")
            )
            val speedkmh = nmea.speedKnots * 1.852
            InfoRow(
                label = "Speed",
                value = listOf(
                    nmea.speedKnots.let { "%.1f kn".format(it) },
                    speedkmh.let { "%.1f km/h".format(it) }
                ).joinToString(" / ")
            )
            InfoRow(
                label = "Course",
                value = if (nmea.course == 0.0) "N/A"
                else nmea.course.let { "%.1f°".format(it) }
            )
            InfoRow(
                label = "Magnetic variation",
                value = if (nmea.magneticVariation == 0.0) "N/A"
                else nmea.magneticVariation.let { "%.1f°".format(it) }
            )
        }
    }
}

@Composable
fun AndroidApiLocationCard(
    locationData: ListenerData,
    currentSystemTime: String,
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.Companion.padding(12.dp)) {
            Row(
                modifier = Modifier.Companion
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Location Listener",
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 20.sp
                )
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Location Settings",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(enabled = onSettingsClick != null) {
                            onSettingsClick?.invoke()
                        }
                )
            }

            Spacer(Modifier.Companion.height(8.dp))

            InfoRow(
                label = "Current system time",
                value = currentSystemTime
            )
            InfoRow(
                label = "Last update",
                value = if (locationData.time == "") "N/A"
                else locationData.time
            )
            InfoRow(
                label = "Provider",
                value = if (locationData.provider == "") "N/A"
                else locationData.provider
            )
            InfoRow(
                label = "Latitude",
                value = if (locationData.latitude == 0.0) "N/A"
                else CoordinateConverter.decimalToDMS(locationData.latitude, locationData.latHemisphere)
            )
            InfoRow(
                label = "Longitude",
                value = if (locationData.longitude == 0.0) "N/A"
                else CoordinateConverter.decimalToDMS(locationData.longitude, locationData.longHemisphere)
            )
            InfoRow(
                label = "Accuracy (2D)", value =
                    if (locationData.accuracy == 0f) "Do data"
                    else "%.1f m".format(locationData.accuracy)
            )
            InfoRow(
                label = "Altitude (MSL)",
                value = if (locationData.altitude == 0.0) "N/A"
                else "%.1f m".format(locationData.altitude)
            )
            InfoRow(
                label = "Vertical Accuracy",
                value = if (locationData.verticalAccuracy == null) "N/A"
                else "%.1f m".format(locationData.verticalAccuracy)
            )
            InfoRow(
                label = "Speed",
                value = if (locationData.speed == 0f) "N/A"
                else "%.2f m/s".format(locationData.speed)
            )
            InfoRow(
                label = "Speed Accuracy",
                value = if (locationData.speedAccuracy == null) "N/A"
                else "%.2f m/s".format(locationData.speedAccuracy)
            )
            InfoRow(
                label = "Bearing",
                value = if (locationData.bearing == 0f) "N/A"
                else "%.2f m/s".format(locationData.bearing)
            )
            InfoRow(
                label = "Bearing Accuracy",
                value = if (locationData.bearingAccuracy == null) "N/A"
                else "%.2f m/s".format(locationData.bearingAccuracy)
            )
            InfoRow(
                label = "Elapsed Time (ns)",
                value = "%,d".format(locationData.elapsedRealtimeNanos)
            )
        }
    }
}
