package com.example.gpssatelliteviewer.mainscreen.screen.locationinfo

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
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
import com.example.gpssatelliteviewer.app.theme.GPSDisabled
import com.example.gpssatelliteviewer.app.theme.GPSExcellent
import com.example.gpssatelliteviewer.app.theme.GPSFair
import com.example.gpssatelliteviewer.app.theme.GPSGood
import com.example.gpssatelliteviewer.app.theme.GPSNoFix
import com.example.gpssatelliteviewer.app.theme.GPSPoor
import com.example.gpssatelliteviewer.app.theme.GPSSearching
import com.example.gpssatelliteviewer.app.theme.SNRDarkerGreen
import com.example.gpssatelliteviewer.app.theme.SNRLightGreen
import com.example.gpssatelliteviewer.app.theme.SNROrange
import com.example.gpssatelliteviewer.app.theme.SNRRed
import com.example.gpssatelliteviewer.app.theme.SNRYellow
import com.example.gpssatelliteviewer.app.theme.TextPrimary
import com.example.gpssatelliteviewer.data.GNSSStatusData

class GPSStatus(
    private val satellites: List<GNSSStatusData>,
    private val hasLocation: Boolean,
    private val isLocationEnabled: Boolean
) {
    sealed class GPSStatusState {
        object Excellent : GPSStatusState()      // Strong signal, many satellites, high accuracy
        object Good : GPSStatusState()           // Good signal, adequate satellites
        object Fair : GPSStatusState()           // Weak signal, few satellites
        object Poor : GPSStatusState()           // Very weak signal, poor accuracy
        object NoFix : GPSStatusState()          // No GPS fix available
        object Searching : GPSStatusState()      // Searching for satellites
        object Disabled : GPSStatusState()       // GPS is turned off
    }

    val averageSNRByConstellationInFix = calculateAverageSNRByConstellationInFix(satellites)
    val averageSNRByConstellation = calculateAverageSNRByConstellation(satellites)
    val averageSNRInFix = calculateAverageSNRInFix(satellites)
    val gpsStatusState = determineGPSStatusState()

    private fun determineGPSStatusState(): GPSStatusState {
        if (!isLocationEnabled) {
            return GPSStatusState.Disabled
        }

        if (!hasLocation) {
            return GPSStatusState.NoFix
        }

        val satellitesUsedInFix = satellites.count { it.usedInFix }

        return when {
            satellitesUsedInFix >= 20 && averageSNRInFix >= 30f -> {
                GPSStatusState.Excellent
            }
            satellitesUsedInFix >= 10 && averageSNRInFix >= 22f -> {
                GPSStatusState.Good
            }
            satellitesUsedInFix >= 4 && averageSNRInFix >= 15f -> {
                GPSStatusState.Fair
            }
            satellitesUsedInFix >= 1 && averageSNRInFix >= 5f -> {
                GPSStatusState.Poor
            }
            satellitesUsedInFix == 0 && averageSNRInFix == 0f -> {
                GPSStatusState.Searching
            }
            else -> GPSStatusState.NoFix
        }
    }

    private fun calculateAverageSNRByConstellationInFix(satellites: List<GNSSStatusData>): Map<String, Float> {
        val satellitesInFix = satellites.filter { it.usedInFix }
        val groupedSatellites = satellitesInFix.groupBy { it.constellation }

        return groupedSatellites.mapValues { (_, sats) ->
            val valid = sats.filter { it.cn0DbHz != 0f }

            if (valid.isNotEmpty()) {
                valid.map { it.cn0DbHz }.average().toFloat()
            } else {
                0.0f
            }
        }
    }

    private fun calculateAverageSNRByConstellation(satellites: List<GNSSStatusData>): Map<String, Float> {
        val groupedSatellites = satellites.groupBy { it.constellation }

        return groupedSatellites.mapValues { (_, sats) ->
            val valid = sats.filter { it.cn0DbHz != 0f }

            if (valid.isNotEmpty()) {
                valid.map { it.cn0DbHz }.average().toFloat()
            } else {
                0.0f
            }
        }
    }

    private fun calculateAverageSNRInFix(satellites: List<GNSSStatusData>): Float {
        val satellitesInFix = satellites.filter { it.usedInFix }
        return if (satellitesInFix.isNotEmpty()) {
            satellitesInFix.map { it.cn0DbHz }.average().toFloat()
        } else 0.0f
    }

    fun getFixCount(): Int {
        return satellites.count { it.usedInFix }
    }

    fun getFixCountByConstellation(constellation: String): Int {
        val satellitesInFix = satellites.filter { it.usedInFix }
        return satellitesInFix.count { it.constellation == constellation }
    }

    fun getSNRNot0CountByConstellation(constellation: String): Int {
        val satelliteSNRNot0 = satellites.filter { it.cn0DbHz != 0f }
        return satelliteSNRNot0.count() { it.constellation == constellation}
    }

    fun getGroupedSatelliteCount(constellation: String): Int {
        return satellites.count() { it.constellation == constellation }
    }

    fun getSatelliteCount(): Int {
        return satellites.size
    }

    // Helper functions for GPS status display
    private fun getStatusIcon(gpsStatusState: GPSStatusState): ImageVector {
        return when (gpsStatusState) {
            is GPSStatusState.Excellent -> Icons.Default.CheckCircle
            is GPSStatusState.Good -> Icons.Default.SignalCellularAlt
            is GPSStatusState.Fair -> Icons.Default.SignalCellularAlt2Bar
            is GPSStatusState.Poor -> Icons.Default.SignalCellularAlt1Bar
            is GPSStatusState.NoFix -> Icons.Default.LocationDisabled
            is GPSStatusState.Searching -> Icons.Default.LocationSearching
            is GPSStatusState.Disabled -> Icons.Default.LocationOff
        }
    }

    private fun getStatusTitle(gpsStatusState: GPSStatusState): String {
        return when (gpsStatusState) {
            is GPSStatusState.Excellent -> "Excellent Signal"
            is GPSStatusState.Good -> "Good Signal"
            is GPSStatusState.Fair -> "Fair Signal"
            is GPSStatusState.Poor -> "Poor Signal"
            is GPSStatusState.NoFix -> "No GPS Fix"
            is GPSStatusState.Searching -> "Searching..."
            is GPSStatusState.Disabled -> "GPS Disabled"
        }
    }

    private fun getStatusColor(gpsStatusState: GPSStatusState): Color {
        return when (gpsStatusState) {
            is GPSStatusState.Excellent -> GPSExcellent
            is GPSStatusState.Good -> GPSGood
            is GPSStatusState.Fair -> GPSFair
            is GPSStatusState.Poor -> GPSPoor
            is GPSStatusState.NoFix -> GPSNoFix
            is GPSStatusState.Searching -> GPSSearching
            is GPSStatusState.Disabled -> GPSDisabled
        }
    }

    @Composable
    fun GPSStatusHeader(
        modifier: Modifier = Modifier
    ) {
        val statusColor = getStatusColor(gpsStatusState)

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
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getStatusIcon(gpsStatusState),
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = getStatusTitle(gpsStatusState),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = statusColor
            )
        }
    }

    @SuppressLint("UnusedBoxWithConstraintsScope")
    @Composable
    fun SNRBar(
        modifier: Modifier = Modifier
    ) {
        val clamped = averageSNRInFix.coerceIn(0f, 99f)

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
}