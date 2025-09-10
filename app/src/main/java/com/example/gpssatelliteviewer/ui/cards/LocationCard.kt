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
import com.example.gpssatelliteviewer.data.NMEAData
import com.example.gpssatelliteviewer.utils.CoordinateConversion
import com.example.gpssatelliteviewer.utils.InfoRow
import com.example.gpssatelliteviewer.utils.approximateLocationAccuracy


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NMEALocationCard(
    nmea: NMEAData,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.Companion.padding(12.dp)) {
            Text(
                "Lokalizacja NMEA",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(8.dp))

            InfoRow(
                label = "Fix",
                value = listOf(
                    nmea.fixQuality,
                    if (nmea.fixQuality == "Brak danych") "No fix"
                    else nmea.numSatellites.let { if (it > 4) "3D fix" else "fix" }
                ).joinToString(" / ")
            )

            Spacer(Modifier.height(6.dp))

            InfoRow(
                label = "Ostatnia aktualizacja (UTC)",
                value = if (nmea.time == "") "Brak danych"
                else nmea.time
            )
            InfoRow(
                label = "Data",
                value = if (nmea.date == "") "Brak danych"
                else nmea.date
            )
            InfoRow(
                label = "Szerokość geograficzna",
                value = if (nmea.latitude == 0.0) "Brak danych"
                else CoordinateConversion.geodeticToDMS(nmea.latitude, nmea.latHemisphere)
            )
            InfoRow(
                label = "Długość geograficzna",
                value = if (nmea.longitude == 0.0) "Brak danych"
                else CoordinateConversion.geodeticToDMS(nmea.longitude, nmea.lonHemisphere)
            )
            InfoRow(
                label = "Wysokość MSL",
                value = nmea.mslAltitude.let { "%.1f m".format(it) }
            )
            /*InfoRow(
                    label = "Wysokość geoidy nad elipsoidą",
                    value = nmea.geoidHeight?.let { "%.1f m".format(it) }
                        ?: "Brak danych")*/
            InfoRow(
                label = "Dokładność",
                value = approximateLocationAccuracy(nmea)
            )
            /*
                InfoRow(
                    label = "Liczba używanych satelitów",
                    value = nmea.numSatellites?.toString() ?: "Brak danych"
                )
                InfoRow(
                    label = "Jakość fix",
                    value = nmea.fixQuality ?: "Brak danych"
                )*/

            val speedkmh = nmea.speedKnots.times(1.852)
            InfoRow(
                label = "Prędkość",
                value = listOf(
                    nmea.speedKnots.let { "%.1f kn".format(it) },
                    speedkmh.let { "%.1f km/h".format(it) }
                ).joinToString(" / ")
            )
            InfoRow(
                label = "Kurs",
                value = nmea.course.let { "%.1f°".format(it) }
            )
            InfoRow(
                label = "Odchylenie magnetyczne",
                value = nmea.magneticVariation.let { "%.1f°".format(it) })

        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
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
        Column(Modifier.Companion.padding(12.dp)) {
            val data = locationAndroidApi
            Text(
                "Lokalizacja Szybka",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.Companion.height(8.dp))

            InfoRow(label = "Ostatnia aktualizacja (UTC)", value = data.time)
            InfoRow(
                label = "Szerokość geograficzna",
                value = "%.6f° ${data.latHemisphere}".format(data.latitude)
            )
            InfoRow(
                label = "Długość geograficzna",
                value = "%.6f° ${data.longHemisphere}".format(data.longitude)
            )
            InfoRow(
                label = "Wysokość m.n.p.m.",
                value = "%.1f m".format(data.altitude)
            )
        }
    }
}

@Composable
fun LoadingLocationText(baseText: String = "Oczekiwanie na lokalizację") {
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
    Text("$baseText$dots", style = MaterialTheme.typography.bodyMedium)
}