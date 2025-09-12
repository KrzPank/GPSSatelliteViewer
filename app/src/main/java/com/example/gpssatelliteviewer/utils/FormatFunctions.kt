package com.example.gpssatelliteviewer.utils

import android.annotation.SuppressLint
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SNRBar(
    value: Float,
    modifier: Modifier = Modifier
) {
    val clamped = value.coerceIn(0f, 99f)

    val ranges = listOf(
        0f to 10f to Color.Companion.Red,
        10f to 20f to Color(0xFFFF9800),
        20f to 30f to Color.Companion.Yellow,
        30f to 50f to Color(0xFF86D317),
        50f to 99f to Color(0xFF39BB3F)
    )

    val thresholds = listOf(0f, 10f, 20f, 30f, 50f, 99f)

    Column(modifier) {
        BoxWithConstraints(
            Modifier.Companion
                .fillMaxWidth()
                .height(17.dp)
        ) {
            val barWidth = constraints.maxWidth.toFloat()
            val percent = clamped / 99f
            val indicatorX = barWidth * percent

            Canvas(Modifier.Companion.fillMaxSize()) {
                ranges.forEach { (range, color) ->
                    val (start, end) = range
                    val startX = (start / 99f) * size.width
                    val endX = (end / 99f) * size.width
                    drawRect(
                        color = color,
                        topLeft = Offset(startX, 8f),
                        size = Size(endX - startX, size.height - 8f)
                    )
                }

                val triangleWidth = 12.dp.toPx()
                val path = Path().apply {
                    moveTo(indicatorX, 8f) // tip
                    lineTo(indicatorX - triangleWidth / 2, -8f)
                    lineTo(indicatorX + triangleWidth / 2, -8f)
                    close()
                }
                drawPath(path, color = Color.Companion.Black)
            }
        }


        BoxWithConstraints(
            Modifier.Companion
                .fillMaxWidth()
                .padding(top = 2.dp)
        ) {
            val barWidth = constraints.maxWidth.toFloat()
            thresholds.forEach { t ->
                val x = (t / 99f) * barWidth
                Text(
                    text = t.toInt().toString(),
                    fontSize = 12.sp,
                    modifier = Modifier.Companion
                        .offset { IntOffset(x.toInt(), -15) }
                )
            }
        }
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
        1 -> "Not available"
        2 -> "2D Fix"
        3 -> "3D Fix"
        else -> "Unknown"
    }
}