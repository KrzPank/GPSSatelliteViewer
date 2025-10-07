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
import com.example.gpssatelliteviewer.data.ListenerData
import com.example.gpssatelliteviewer.data.NMEALocationData
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import com.example.gpssatelliteviewer.utils.InfoRow
import com.example.gpssatelliteviewer.utils.mapFixQuality
import com.example.gpssatelliteviewer.utils.mapFixType

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NMEALocationCard(
    nmea: NMEALocationData,
    currentSystemTime: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (onClick != null) Modifier.Companion.clickable { onClick() } else Modifier.Companion),
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
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp
                )
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Location Settings",
                    modifier = Modifier.Companion.size(24.dp)
                )
            }

            Spacer(Modifier.Companion.height(8.dp))

            InfoRow(
                label = "Current system time",
                value = currentSystemTime
            )
            InfoRow(
                label = "Last update (UTC)",
                value = if (nmea.time == "") "No data"
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
                value = if (nmea.date == "") "No data"
                else nmea.date
            )
            InfoRow(
                label = "Latitude",
                value = if (nmea.latitude == 0.0) "No data"
                else CoordinateConverter.nmeaCoordinateToDMS(nmea.latitude, nmea.latHemisphere)
            )
            InfoRow(
                label = "Longitude",
                value = if (nmea.longitude == 0.0) "No data"
                else CoordinateConverter.nmeaCoordinateToDMS(nmea.longitude, nmea.lonHemisphere)
            )
            InfoRow(
                label = "Altitude MSL",
                value = nmea.mslAltitude.let { "%.1f m".format(it) }
            )
            InfoRow(
                label = "Accuracy",
                value = CoordinateConverter.getAccuracyEstimate(nmea).let { "%.1f m".format(it) }
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
                value = if (nmea.course == 0.0) "No data"
                else nmea.course.let { "%.1f°".format(it) }
            )
            InfoRow(
                label = "Magnetic variation",
                value = if (nmea.magneticVariation == 0.0) "No data"
                else nmea.magneticVariation.let { "%.1f°".format(it) }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun AndroidApiLocationCard(
    locationData: ListenerData,
    currentSystemTime: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.Companion.clickable { onClick() } else Modifier.Companion),
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
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp
                )
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Location Settings",
                    modifier = Modifier.Companion.size(24.dp)
                )
            }

            Spacer(Modifier.Companion.height(8.dp))

            InfoRow(label = "Current system time", value = currentSystemTime)
            InfoRow(
                label = "Last update",
                value = if (locationData.time == "") "No data"
                else locationData.time
            )
            InfoRow(
                label = "Latitude",
                value = if (locationData.latitude == 0.0) "No Data"
                else CoordinateConverter.decimalToDMS(locationData.latitude, locationData.latHemisphere)
            )
            InfoRow(
                label = "Longitude",
                value = if (locationData.longitude == 0.0) "No Data"
                else CoordinateConverter.decimalToDMS(locationData.longitude, locationData.longHemisphere)
            )
            InfoRow(
                label = "Altitude (MSL)",
                value = if (locationData.altitude == 0.0) "No data"
                else "%.1f m".format(locationData.altitude)
            )
            InfoRow(
                label = "Accuracy (2D)", value =
                    if (locationData.accuracy == 0f) "Do data"
                    else "%.1f m".format(locationData.accuracy)
            )
            InfoRow(
                label = "Speed",
                value = if (locationData.speed == 0f) "No data"
                else "%.2f m/s".format(locationData.speed)
            )
            InfoRow(
                label = "Provider",
                value = if (locationData.provider == "") "No data"
                else locationData.provider
            )
            InfoRow(
                label = "Vertical Accuracy",
                value = if (locationData.verticalAccuracy == null) "No data"
                else "%.1f m".format(locationData.verticalAccuracy)
            )
            InfoRow(
                label = "Speed Accuracy",
                value = if (locationData.speedAccuracy == null) "No data"
                else "%.2f m/s".format(locationData.speedAccuracy)
            )
            InfoRow(
                label = "Bearing",
                value = if (locationData.bearing == 0f) "No data"
                else "%.2f m/s".format(locationData.bearing)
            )
            InfoRow(
                label = "Bearing Accuracy",
                value = if (locationData.bearingAccuracy == null) "No data"
                else "%.2f m/s".format(locationData.bearingAccuracy)
            )
            InfoRow(
                label = "Elapsed Time (ns)",
                value = "%,d".format(locationData.elapsedRealtimeNanos)
            )
        }
    }
}
