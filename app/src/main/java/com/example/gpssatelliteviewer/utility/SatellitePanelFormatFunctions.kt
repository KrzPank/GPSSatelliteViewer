package com.example.gpssatelliteviewer.utility

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

fun formatNmeaTime(nmeaTime: String): String {
    return try {
        if (nmeaTime.length >= 6) {
            val hh = nmeaTime.substring(0, 2)
            val mm = nmeaTime.substring(2, 4)
            val ss = nmeaTime.substring(4, 6)
            "$hh:$mm:$ss"
        } else nmeaTime
    } catch (e: Exception) {
        nmeaTime
    }
}

fun mapFixQuality(fixQuality: Int): String {
    return when(fixQuality) {
        0 -> "Brak fixu"
        1 -> "GPS fix"
        2 -> "DGPS fix"
        3 -> "PPS fix"
        4 -> "RTK"
        5 -> "Float RTK"
        6 -> "Estymowany"
        7 -> "Manualny"
        8 -> "Simulowany"
        else -> "Nieznany"
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