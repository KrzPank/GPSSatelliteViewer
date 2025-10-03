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
import com.example.gpssatelliteviewer.ui.theme.SNRDarkerGreen
import com.example.gpssatelliteviewer.ui.theme.SNRLightGreen
import com.example.gpssatelliteviewer.ui.theme.SNROrange
import com.example.gpssatelliteviewer.ui.theme.SNRRed
import com.example.gpssatelliteviewer.ui.theme.SNRYellow
import com.example.gpssatelliteviewer.ui.theme.GPSExcellent
import com.example.gpssatelliteviewer.ui.theme.GPSGood
import com.example.gpssatelliteviewer.ui.theme.GPSFair
import com.example.gpssatelliteviewer.ui.theme.GPSPoor
import com.example.gpssatelliteviewer.ui.theme.GPSNoFix
import com.example.gpssatelliteviewer.ui.theme.GPSSearching
import com.example.gpssatelliteviewer.ui.theme.GPSDisabled
import com.example.gpssatelliteviewer.ui.theme.TextPrimary

sealed class GPSStatus {
    object Excellent : GPSStatus()      // Strong signal, many satellites, high accuracy
    object Good : GPSStatus()           // Good signal, adequate satellites
    object Fair : GPSStatus()           // Weak signal, few satellites
    object Poor : GPSStatus()           // Very weak signal, poor accuracy
    object NoFix : GPSStatus()          // No GPS fix available
    object Searching : GPSStatus()      // Searching for satellites
    object Disabled : GPSStatus()       // GPS is turned off
}

data class SNRStats(
    val average: Float,
    val count: Float
)

object GPSStatusUtils {
    fun determineGPSStatus(
        satellites: List<GNSSStatusData>,
        averageSnr: Float,
        hasLocationNMEA: Boolean,
    ): GPSStatus {
        if (!hasLocationNMEA && satellites.isEmpty()) {
            return GPSStatus.Searching
        }

        if (satellites.isEmpty()) {
            return GPSStatus.NoFix
        }

        val satellitesUsedInFix = satellites.count { it.usedInFix }
        
        return when {
            // Excellent: Many satellites, good SNR, many used in fix
            satellitesUsedInFix >= 20 && averageSnr >= 32f -> {
                GPSStatus.Excellent
            }
            // Good: Adequate satellites, decent SNR
            satellitesUsedInFix >= 10 && averageSnr >= 25f -> {
                GPSStatus.Good
            }
            // Fair: Some satellites, moderate SNR
            satellitesUsedInFix >= 6 && averageSnr >= 17f -> {
                GPSStatus.Fair
            }
            // Poor: Few satellites or weak signal
            satellitesUsedInFix >= 1 && averageSnr >= 10f ->{
                GPSStatus.Poor
            }
            // TODO ADD searching and disabled GPSStatus
            else -> GPSStatus.NoFix
        }
    }

    fun calculateAverageSNRByConstellation(satellites: List<GNSSStatusData>): Map<String, SNRStats> {
        val groupedSatellites = satellites.groupBy { it.constellation }
        return groupedSatellites.mapValues { (_, sats) ->
            val valid = sats.filter { it.snr != 0f }
            if (valid.isNotEmpty()) {
                val avg = valid.map { it.snr }.average().toFloat()
                val count = valid.size.toFloat()
                SNRStats(avg, count)
            } else {
                SNRStats(0f, 0f)
            }
        }
    }

    fun calculateAverageSNRInFix(satellites: List<GNSSStatusData>): Float {
        val satellitesInFix = satellites.filter { it.usedInFix }
        return if (satellitesInFix.isNotEmpty()) {
            satellitesInFix.map { it.snr }.average().toFloat()
        } else 0f
    }

    fun getUsedInFixCount(satellites: List<GNSSStatusData>): Int {
        return satellites.count { it.usedInFix }
    }

    fun getTotalSatelliteCount(satellites: List<GNSSStatusData>): Int {
        return satellites.size
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
            is GPSStatus.Excellent -> GPSExcellent
            is GPSStatus.Good -> GPSGood
            is GPSStatus.Fair -> GPSFair
            is GPSStatus.Poor -> GPSPoor
            is GPSStatus.NoFix -> GPSNoFix
            is GPSStatus.Searching -> GPSSearching
            is GPSStatus.Disabled -> GPSDisabled
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
            0f to 10f to SNRRed,
            10f to 20f to SNROrange,
            20f to 30f to SNRYellow,
            30f to 50f to SNRLightGreen,
            50f to 99f to SNRDarkerGreen
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
                    drawPath(path, color = TextPrimary)
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
