package com.example.gpssatelliteviewer.ui.cards

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpssatelliteviewer.data.ListenerData
import com.example.gpssatelliteviewer.data.NMEALocationData
import com.example.gpssatelliteviewer.data.parsers.NMEAParser
import com.example.gpssatelliteviewer.utils.CoordinateConversion
import com.example.gpssatelliteviewer.utils.InfoRow
import com.example.gpssatelliteviewer.utils.approximateLocationAccuracy
import com.example.gpssatelliteviewer.utils.mapFixQuality
import com.example.gpssatelliteviewer.utils.mapFixType


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NMEALocationCard(
    nmea: NMEALocationData,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Current location",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(8.dp))

            InfoRow(
                label = "Fix / Type",
                value = listOf(
                    mapFixQuality(nmea.fixQuality),
                    mapFixType(nmea.fixType)
                ).joinToString(" / ")
            )

            //Spacer(Modifier.height(6.dp))

            InfoRow(
                label = "Last update (UTC)",
                value = if (nmea.time == "") "No data"
                else nmea.time
            )
            InfoRow(
                label = "Date",
                value = if (nmea.date == "") "No data"
                else nmea.date
            )
            InfoRow(
                label = "Latitude",
                value = if (nmea.latitude == 0.0) "No data"
                else CoordinateConversion.geodeticToDMS(nmea.latitude, nmea.latHemisphere)
            )
            InfoRow(
                label = "Longitude",
                value = if (nmea.longitude == 0.0) "No data"
                else CoordinateConversion.geodeticToDMS(nmea.longitude, nmea.lonHemisphere)
            )
            InfoRow(
                label = "Altitude MSL",
                value = nmea.mslAltitude.let { "%.1f m".format(it) }
            )
            InfoRow(
                label = "Accuracy",
                value = approximateLocationAccuracy(nmea)
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
fun AndroidApiLocation(
    locationAndroidApi: ListenerData,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            val data = locationAndroidApi
            Text(
                "Quick Location",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            InfoRow(label = "Last update (UTC)", value = data.time)
            InfoRow(
                label = "Latitude",
                value = "%.6f° ${data.latHemisphere}".format(data.latitude)
            )
            InfoRow(
                label = "Longitude",
                value = "%.6f° ${data.longHemisphere}".format(data.longitude)
            )
            InfoRow(
                label = "Altitude (MSL)",
                value = "%.1f m".format(data.altitude)
            )
        }
    }
}

@Composable
fun LoadingLocationText(baseText: String = "Waiting for location") {
    val dotCount = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            dotCount.animateTo(
                targetValue = (dotCount.value + 1) % 4,
                animationSpec = tween(durationMillis = 500, easing = LinearEasing)
            )
        }
    }

    val dots = ".".repeat(dotCount.value.toInt())
    Card {
        InfoRow(
            label = "$baseText$dots",
            value = ""
        )
    }
}