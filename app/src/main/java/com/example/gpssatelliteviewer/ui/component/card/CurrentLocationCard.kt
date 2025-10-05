package com.example.gpssatelliteviewer.ui.component.card

import android.icu.text.SimpleDateFormat
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
// Removed LaunchedEffect import

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpssatelliteviewer.data.ListenerData
import com.example.gpssatelliteviewer.data.NMEALocationData
import com.example.gpssatelliteviewer.utils.CoordinateConverter
import com.example.gpssatelliteviewer.utils.InfoRow
import com.example.gpssatelliteviewer.utils.mapFixQuality
import com.example.gpssatelliteviewer.utils.mapFixType
import java.sql.Date
import java.util.Locale


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NMEALocationCard(
    nmea: NMEALocationData,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
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
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            InfoRow(
                label = "Fix / Type",
                value = listOf(
                    mapFixQuality(nmea.fixQuality),
                    mapFixType(nmea.fixType)
                ).joinToString(" / ")
            )

            InfoRow(
                label = "Last update (UTC)",
                value = if (nmea.time == "") "No data"
                else nmea.time
            )
            val timestampMillis = System.currentTimeMillis() + 1000
            val date = Date(timestampMillis)
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            InfoRow(
                label = "Current system time",
                value = sdf.format(date).toString()
            )
            InfoRow(
                label = "Date",
                value = if (nmea.date == "") "No data"
                else nmea.date
            )
            InfoRow(
                label = "Latitude",
                value = if (nmea.latitude == 0.0) "No data"
                else CoordinateConverter.geodeticToDMS(nmea.latitude, nmea.latHemisphere)
            )
            InfoRow(
                label = "Longitude",
                value = if (nmea.longitude == 0.0) "No data"
                else CoordinateConverter.geodeticToDMS(nmea.longitude, nmea.lonHemisphere)
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
                value = if (nmea.course == 0.0) "-"
                    else nmea.course.let { "%.1f°".format(it) }
            )
            InfoRow(
                label = "Magnetic variation",
                value = if (nmea.magneticVariation == 0.0) "-"
                    else nmea.magneticVariation.let { "%.1f°".format(it) }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun AndroidApiLocationCard(
    locationData: ListenerData,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Quick Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp
                )
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Location Settings",
                    modifier = Modifier.size(24.dp) // 16.dp is very small, usually 24.dp looks better
                )
            }

            Spacer(Modifier.height(8.dp))

            // --- Basic Location Info ---
            val timestampMillis = System.currentTimeMillis() + 1000
            val date = Date(timestampMillis)
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            InfoRow(label = "Current system time", value = sdf.format(date).toString())
            InfoRow(label = "Last update (UTC)", value = locationData.time)
            InfoRow(
                label = "Latitude",
                value = if (locationData.latitude == 0.0) "No Data"
                else CoordinateConverter.geodeticToDMS(locationData.latitude, locationData.latHemisphere)
            )
            InfoRow(
                label = "Longitude",
                value = if (locationData.longitude == 0.0) "No Data" else
                    CoordinateConverter.geodeticToDMS(locationData.longitude, locationData.longHemisphere)
            )
            InfoRow(label = "Altitude (MSL)", value = "%.1f m".format(locationData.altitude))
            InfoRow(label = "Accuracy (2D)", value = "%.1f m".format(locationData.accuracy))
            InfoRow(label = "Speed", value = "%.2f m/s".format(locationData.speed))
            InfoRow(label = "Provider", value = locationData.provider)

            // --- Optional / Extended Data ---
            if (locationData.verticalAccuracy != null)
                InfoRow(label = "Vertical Accuracy", value = "%.1f m".format(locationData.verticalAccuracy))
            if (locationData.speedAccuracy != null)
                InfoRow(label = "Speed Accuracy", value = "%.2f m/s".format(locationData.speedAccuracy))
            if (locationData.bearing != 0f)
                InfoRow(label = "Bearing", value = "%.1f°".format(locationData.bearing))
            if (locationData.bearingAccuracy != null)
                InfoRow(label = "Bearing Accuracy", value = "%.1f°".format(locationData.bearingAccuracy))

            Spacer(Modifier.height(4.dp))

            InfoRow(
                label = "Elapsed Time (ns)",
                value = "%,d".format(locationData.elapsedRealtimeNanos)
            )
        }
    }
}

@Composable
fun LoadingLocationTextCard(
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        InfoRow(
            label = "Waiting for location...",
            value = ""
        )
    }
}
