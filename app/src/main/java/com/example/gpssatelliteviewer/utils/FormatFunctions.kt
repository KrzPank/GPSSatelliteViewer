package com.example.gpssatelliteviewer.utils

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.Companion.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 15.sp
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Companion.Bold,
            fontSize = 17.sp
        )
    }
}

fun mapFixQuality(fixQuality: Int): String {
    return when(fixQuality) {
        0 -> "No fix"
        1 -> "GPS"
        2 -> "DGPS"
        3 -> "PPS"
        4 -> "RTK"
        5 -> "Float RTK"
        6 -> "Estimated"
        7 -> "Manual"
        8 -> "Simulated"
        else -> "Unknown"
    }
}
fun mapFixType(fixType: Int): String {
    return when(fixType) {
        1 -> "N/A"
        2 -> "2D Fix"
        3 -> "3D Fix"
        else -> "Unknown"
    }
}

fun mapTalker(talker: String): String {
    return when (talker) {
        "GPGSV" -> "GPS/SBAS"
        "GLGSV" -> "GLONASS"
        "GBGSV" -> "BeiDou"
        "GAGSV" -> "Galileo"
        "GQGSV" -> "QZSS"
        else -> "Unknown"
    }
}
