package com.example.gpssatelliteviewer.utils

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.data.NMEALocationData

/**
 * Represents different GPS status levels for visual feedback
 */
sealed class GPSStatus {
    object Excellent : GPSStatus()      // Strong signal, many satellites, high accuracy
    object Good : GPSStatus()           // Good signal, adequate satellites
    object Fair : GPSStatus()           // Weak signal, few satellites
    object Poor : GPSStatus()           // Very weak signal, poor accuracy
    object NoFix : GPSStatus()          // No GPS fix available
    object Searching : GPSStatus()      // Searching for satellites
    object Disabled : GPSStatus()       // GPS is turned off
}

/**
 * Utility functions for determining GPS status
 */
object GPSStatusUtils {
    /**
     * Determines the current GPS status based on satellite and location data
     */
    fun determineGPSStatus(
        satellites: List<GNSSStatusData>,
        averageSnr: Float,
        hasLocationNMEA: Boolean,
    ): GPSStatus {
        // Check if GPS is completely disabled or no data
        if (!hasLocationNMEA && satellites.isEmpty()) {
            return GPSStatus.Searching
        }
        
        // No satellites visible
        if (satellites.isEmpty()) {
            return GPSStatus.NoFix
        }

        val satellitesUsedInFix = satellites.count { it.usedInFix }
        
        return when {
            // Excellent: Many satellites, good SNR, many used in fix
            satellitesUsedInFix >= 10 && averageSnr >= 35f -> {
                GPSStatus.Excellent
            }
            // Good: Adequate satellites, decent SNR
            satellitesUsedInFix >= 6 && averageSnr >= 28f -> {
                GPSStatus.Good
            }
            // Fair: Some satellites, moderate SNR
            satellitesUsedInFix >= 4 && averageSnr >= 20f -> {
                GPSStatus.Fair
            }
            // Poor: Few satellites or weak signal
            satellitesUsedInFix >= 1 && averageSnr >= 10f ->{
                GPSStatus.Poor
            }
            else -> GPSStatus.NoFix
        }
    }

    /**
     * Calculates signal strength as a percentage (0.0 to 1.0)
     */
    fun calculateSignalStrength(satellites: List<GNSSStatusData>): Float {
        if (satellites.isEmpty()) return 0f
        
        val averageSnr = satellites.map { it.snr }.average().toFloat()
        // Normalize SNR to 0-1 range (assuming 50 dBHz is excellent)
        return (averageSnr / 50f).coerceIn(0f, 1f)
    }

    // Helper functions for GPS status display
    fun getStatusIcon(gpsStatus: GPSStatus): ImageVector {
        return when (gpsStatus) {
            is GPSStatus.Excellent, is GPSStatus.Good -> Icons.Default.Check
            is GPSStatus.Fair -> Icons.Default.Home
            is GPSStatus.Poor -> Icons.Default.Person
            is GPSStatus.NoFix -> Icons.Default.Close
            is GPSStatus.Searching -> Icons.Default.Search
            is GPSStatus.Disabled -> Icons.Default.Close
        }
    }

    fun getStatusTitle(gpsStatus: GPSStatus): String {
        return when (gpsStatus) {
            is GPSStatus.Excellent -> "Excellent Signal"
            is GPSStatus.Good -> "Good Signal"
            is GPSStatus.Fair -> "Fair Signal"
            is GPSStatus.Poor -> "Poor Signal"
            is GPSStatus.NoFix -> "No GPS Fix"
            is GPSStatus.Searching -> "Searching..."
            is GPSStatus.Disabled -> "GPS Disabled"
        }
    }
    
    fun getStatusColor(gpsStatus: GPSStatus): Color {
        return when (gpsStatus) {
            is GPSStatus.Excellent -> Color(0xFF4CAF50) // Green
            is GPSStatus.Good -> Color(0xFF8BC34A)       // Light Green
            is GPSStatus.Fair -> Color(0xFFFF9800)       // Orange
            is GPSStatus.Poor -> Color(0xFFFF5722)       // Red Orange
            is GPSStatus.NoFix -> Color(0xFFF44336)      // Red
            is GPSStatus.Searching -> Color(0xFF2196F3)  // Blue
            is GPSStatus.Disabled -> Color(0xFF9E9E9E)   // Gray
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
            0f to 10f to Color.Red,
            10f to 20f to Color(0xFFFF9800),
            20f to 30f to Color.Yellow,
            30f to 50f to Color(0xFF86D317),
            50f to 99f to Color(0xFF39BB3F)
        )

        val thresholds = listOf(0f, 10f, 20f, 30f, 50f, 99f)

        Column(modifier) {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(17.dp)
            ) {
                val barWidth = constraints.maxWidth.toFloat()
                val percent = clamped / 99f
                val indicatorX = barWidth * percent

                Canvas(Modifier.fillMaxSize()) {
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
                    drawPath(path, color = Color.Black)
                }
            }

            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                val barWidth = constraints.maxWidth.toFloat()
                thresholds.forEach { t ->
                    val x = (t / 99f) * barWidth
                    Text(
                        text = t.toInt().toString(),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .offset { IntOffset(x.toInt(), -15) }
                    )
                }
            }
        }
    }
    
    /**
     * GPS Status Header - displays status icon and title in a row
     */
    @Composable
    fun GPSStatusHeader(
        gpsStatus: GPSStatus,
        modifier: Modifier = Modifier
    ) {
        val statusColor = getStatusColor(gpsStatus)
        
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon with colored border
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border(
                        width = 2.dp,
                        color = statusColor,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(
                        color = statusColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getStatusIcon(gpsStatus),
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = getStatusTitle(gpsStatus),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = statusColor
            )
        }
    }
}
