package com.example.gpssatelliteviewer.utility

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.data.GNSSData
import com.example.gpssatelliteviewer.data.ListenerData
import com.example.gpssatelliteviewer.data.NMEAData
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

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

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NMEALocationPanel(nmea: NMEAData, message: String, localTime: String){
    val speedkmh = nmea.speedKnots?.times(1.852)
    Text(message, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    InfoRow(label = "Aktualny czas", value = localTime)
    nmea.time?.let { InfoRow(label = "Ostatnia aktualizacja (UTC)", value = it) }
    nmea.date?.let { InfoRow(label = "Data", value = it) }

    InfoRow(label = "Szerokość geograficzna", value = nmea.latitude ?: "Brak danych")
    InfoRow(label = "Długość geograficzna", value = nmea.longitude ?: "Brak danych")
    InfoRow(label = "Wysokość m.n.p.m.", value = nmea.altitude?.let { "%.1f m".format(it) } ?: "Brak danych")
    InfoRow(label = "Wysokość geoidy nad elipsoidą", value = nmea.geoidHeight?.let { "%.1f m".format(it) } ?: "Brak danych")
    InfoRow(label = "Dokładność", value = approximateLocationAccuracy(nmea))
    InfoRow(label = "Liczba używanych satelitów", value = nmea.numSatellites?.toString() ?: "Brak danych")
    InfoRow(label = "Jakość fix", value = nmea.fixQuality ?: "Brak danych")
    InfoRow(
        label = "Prędkość",
        value = listOf(
            nmea.speedKnots?.let { "%.1f kn".format(it) } ?: "Brak danych",
            speedkmh?.let { "%.1f km/h".format(it) } ?: "Brak danych"
        ).joinToString(" / ")
    )
    InfoRow(label = "Kurs", value = nmea.course?.let { "%.1f°".format(it) } ?: "Brak danych")
    InfoRow(label = "Odchylenie magnetyczne", value = nmea.magneticVariation?.let { "%.1f°".format(it) } ?: "Brak danych")
    InfoRow(label = "Błędy GBS (lat, lon, alt)", value = nmea.gbsErrors?.joinToString(", ") { "%.2f m".format(it) } ?: "Brak danych")
}

@Composable
fun AndroidLocationApiPanel(data: ListenerData, message: String){

    Text("Lokalizacja $message", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    InfoRow(label = "Ostatnia aktualizacja (CET)", value = data.time)
    InfoRow(label = "Szerokość geograficzna", value = "%.6f° ${data.latHemisphere}".format(data.latitude))
    InfoRow(label = "Długość geograficzna", value = "%.6f° ${data.longHemisphere}".format(data.longitude))
    InfoRow(label = "Wysokość m.n.p.m.", value = "%.1f m".format(data.altitude))
}
