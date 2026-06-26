package com.example.gnssmap.mainscreen.screens.locationinfo

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
import androidx.compose.material.icons.filled.Check
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
import com.example.gnssmap.app.theme.GNSSDisabledColor
import com.example.gnssmap.app.theme.GNSSExcellentColor
import com.example.gnssmap.app.theme.GNSSFairColor
import com.example.gnssmap.app.theme.GNSSGoodColor
import com.example.gnssmap.app.theme.GNSSNoFixColor
import com.example.gnssmap.app.theme.GNSSPoorColor
import com.example.gnssmap.app.theme.GNSSSearchingColor
import com.example.gnssmap.app.theme.SNRDarkerGreenColor
import com.example.gnssmap.app.theme.SNRLightGreenColor
import com.example.gnssmap.app.theme.SNROrangeColor
import com.example.gnssmap.app.theme.SNRRedColor
import com.example.gnssmap.app.theme.SNRYellowColor
import com.example.gnssmap.app.theme.TextPrimaryColor
import com.example.gnssmap.data.EXCELLENT_CNO
import com.example.gnssmap.data.FAIR_CNO
import com.example.gnssmap.data.GNSSStatusData
import com.example.gnssmap.data.GOOD_CNO
import com.example.gnssmap.data.NO_CNO
import com.example.gnssmap.data.POOR_CNO

class GNSSStatus(
    private val satellites: List<GNSSStatusData>,
    private val hasLocation: Boolean,
    private val isLocationEnabled: Boolean
) {
    sealed class GNSSStatusState {
        object Excellent : GNSSStatusState()      // Strong signal, many satellites, high accuracy
        object Good : GNSSStatusState()           // Good signal, adequate satellites
        object Fair : GNSSStatusState()           // Weak signal, few satellites
        object Poor : GNSSStatusState()           // Very weak signal, poor accuracy
        object NoFix : GNSSStatusState()          // No GNSS fix available
        object Searching : GNSSStatusState()      // Searching for satellites
        object Disabled : GNSSStatusState()       // GNSS is turned off
    }

    val averageSNRByConstellationInFix = calculateAverageSNRByConstellationInFix(satellites)
    val averageSNRByConstellation = calculateAverageSNRByConstellation(satellites)
    val averageSNRInFix = calculateAverageSNRInFix(satellites)
    val gnssStatusState = determineGNSSStatusState()

    private fun determineGNSSStatusState(): GNSSStatusState {
        if (!isLocationEnabled) {
            return GNSSStatusState.Disabled
        }

        if (!hasLocation) {
            return GNSSStatusState.NoFix
        }

        val satellitesUsedInFix = satellites.count { it.usedInFix }

        return when {
            satellitesUsedInFix >= 25 && averageSNRInFix >= EXCELLENT_CNO -> {
                GNSSStatusState.Excellent
            }
            satellitesUsedInFix >= 15 && averageSNRInFix >= GOOD_CNO -> {
                GNSSStatusState.Good
            }
            satellitesUsedInFix >= 10 && averageSNRInFix >= FAIR_CNO -> {
                GNSSStatusState.Fair
            }
            satellitesUsedInFix >= 4 && averageSNRInFix >= POOR_CNO -> {
                GNSSStatusState.Poor
            }
            satellitesUsedInFix == 0 && averageSNRInFix == NO_CNO -> {
                GNSSStatusState.Searching
            }
            else -> GNSSStatusState.NoFix
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

    // Helper functions for GNSS status display
    private fun getStatusIcon(gnssStatusState: GNSSStatusState): ImageVector {
        return when (gnssStatusState) {
            is GNSSStatusState.Excellent -> Icons.Default.Check
            is GNSSStatusState.Good -> Icons.Default.SignalCellularAlt
            is GNSSStatusState.Fair -> Icons.Default.SignalCellularAlt2Bar
            is GNSSStatusState.Poor -> Icons.Default.SignalCellularAlt1Bar
            is GNSSStatusState.NoFix -> Icons.Default.LocationDisabled
            is GNSSStatusState.Searching -> Icons.Default.LocationSearching
            is GNSSStatusState.Disabled -> Icons.Default.LocationOff
        }
    }

    private fun getStatusTitle(gnssStatusState: GNSSStatusState): String {
        return when (gnssStatusState) {
            is GNSSStatusState.Excellent -> "Excellent Signal"
            is GNSSStatusState.Good -> "Good Signal"
            is GNSSStatusState.Fair -> "Fair Signal"
            is GNSSStatusState.Poor -> "Poor Signal"
            is GNSSStatusState.NoFix -> "No Fix"
            is GNSSStatusState.Searching -> "Searching..."
            is GNSSStatusState.Disabled -> "Location Disabled"
        }
    }

    private fun getStatusColor(gnssStatusState: GNSSStatusState): Color {
        return when (gnssStatusState) {
            is GNSSStatusState.Excellent -> GNSSExcellentColor
            is GNSSStatusState.Good -> GNSSGoodColor
            is GNSSStatusState.Fair -> GNSSFairColor
            is GNSSStatusState.Poor -> GNSSPoorColor
            is GNSSStatusState.NoFix -> GNSSNoFixColor
            is GNSSStatusState.Searching -> GNSSSearchingColor
            is GNSSStatusState.Disabled -> GNSSDisabledColor
        }
    }

    @Composable
    fun GNSSStatusHeader(
        modifier: Modifier = Modifier
    ) {
        val statusColor = getStatusColor(gnssStatusState)

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
                    imageVector = getStatusIcon(gnssStatusState),
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = getStatusTitle(gnssStatusState),
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
            0f to 10f to SNRRedColor,
            10f to 20f to SNROrangeColor,
            20f to 30f to SNRYellowColor,
            30f to 50f to SNRLightGreenColor,
            50f to 99f to SNRDarkerGreenColor
        )

        val thresholds = listOf(0f, 10f, 20f, 30f, 50f, 99f)

        Column(modifier) {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(17.dp)
                    .padding(end = 10.dp)
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
                    drawPath(path, color = TextPrimaryColor)
                }
            }

            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, end = 10.dp)
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