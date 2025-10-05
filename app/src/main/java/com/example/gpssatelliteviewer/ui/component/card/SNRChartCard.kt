package com.example.gpssatelliteviewer.ui.component.card

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gpssatelliteviewer.ui.theme.ChartBeoDou
import com.example.gpssatelliteviewer.ui.theme.ChartGLONASS
import com.example.gpssatelliteviewer.ui.theme.ChartGPS
import com.example.gpssatelliteviewer.ui.theme.ChartGalileo
import com.example.gpssatelliteviewer.ui.theme.ChartIRNSS
import com.example.gpssatelliteviewer.ui.theme.ChartQZSS
import com.example.gpssatelliteviewer.ui.theme.ChartSBAS
import com.example.gpssatelliteviewer.ui.theme.TextPrimary
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.sql.Date
import java.util.Locale

@Composable
fun SNRChartCard(
    snrHistory: Map<String, List<Float>>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Text(
            "Average SNR per constellation in FIX",
            style = MaterialTheme.typography.titleMedium,
            fontSize = 20.sp
        )
        Spacer(Modifier.height(6.dp))

        AndroidView(
            modifier = modifier
                .fillMaxSize()
                .padding(4.dp),
            factory = { context ->
                LineChart(context).apply {
                    description.isEnabled = false
                    axisRight.isEnabled = false
                    setTouchEnabled(false)
                    setPinchZoom(true)

                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        setDrawGridLines(false)
                        granularity = 1f
                        isGranularityEnabled = true
                        axisMinimum = 0f
                        axisMaximum = 50f
                        textColor = TextPrimary.toArgb()
                        textSize = 10f
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                val secondsAgo = (50 - value.toInt()) * 1 // assuming 1 second per entry
                                val timestampMillis = System.currentTimeMillis() - secondsAgo * 1000
                                val date = Date(timestampMillis)
                                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                return if (value % 10 == 0f) sdf.format(date) else ""
                            }
                        }
                    }

                    axisLeft.apply {
                        setDrawGridLines(true)
                        textColor = TextPrimary.toArgb()
                        textSize = 10f
                    }

                    legend.apply {
                        isEnabled = true
                        form = Legend.LegendForm.LINE
                        textColor = TextPrimary.toArgb()
                        textSize = 14f
                    }
                }
            },
            update = { lineChart ->
                val windowSize = 50
                val dataSets = snrHistory.entries.mapIndexedNotNull { index, (constellation, snrList) ->
                    val clipped = if (snrList.size > windowSize) snrList.takeLast(windowSize) else snrList

                    if (clipped.isEmpty()) return@mapIndexedNotNull null // skip empty series

                    val startX = (windowSize - clipped.size).coerceAtLeast(0)
                    val entries = clipped.mapIndexedNotNull { i, snr ->
                        if (snr != 0f) {
                            Entry(startX + i.toFloat(), snr)
                        } else null
                    }

                    LineDataSet(entries, constellation).apply {
                        color = getConstellationColor(constellation).toArgb()
                        setDrawCircles(false)
                        lineWidth = 2f
                        setDrawValues(false)
                        mode = LineDataSet.Mode.LINEAR
                    }
                }

                lineChart.data = LineData(dataSets)
                lineChart.setVisibleXRangeMaximum(windowSize.toFloat())
                lineChart.invalidate() // Refresh the chart
            }
        )
    }
}

private fun getConstellationColor(constellation: String): Color {
    return when (constellation.uppercase()) {
        "GPS" -> ChartGPS
        "GLONASS" -> ChartGLONASS
        "GALILEO" -> ChartGalileo
        "BEIDOU" -> ChartBeoDou
        "QZSS" -> ChartQZSS
        "IRNSS" -> ChartIRNSS
        "SBAS" -> ChartSBAS
        else -> Color.LightGray
    }
}
