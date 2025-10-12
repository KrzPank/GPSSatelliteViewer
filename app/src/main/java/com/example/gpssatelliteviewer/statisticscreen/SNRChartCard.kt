package com.example.gpssatelliteviewer.statisticscreen

import android.content.Context
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gpssatelliteviewer.app.theme.ChartBeoDou
import com.example.gpssatelliteviewer.app.theme.ChartGLONASS
import com.example.gpssatelliteviewer.app.theme.ChartGPS
import com.example.gpssatelliteviewer.app.theme.ChartGalileo
import com.example.gpssatelliteviewer.app.theme.ChartIRNSS
import com.example.gpssatelliteviewer.app.theme.ChartQZSS
import com.example.gpssatelliteviewer.app.theme.ChartSBAS
import com.example.gpssatelliteviewer.app.theme.TextPrimary
import com.example.gpssatelliteviewer.data.CHART_UPDATE_WINDOW
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.sql.Date
import java.util.Locale
import kotlin.math.round

@Composable
fun SNRChartCard(
    snrHistory: Map<String, List<Float>>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(220.dp)
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        AndroidView(
            modifier = modifier
                .fillMaxSize()
                .padding(10.dp),
            factory = { context -> applyChartSettings(context) },
            update = { lineChart ->
                val dataSets =
                    snrHistory.entries.mapIndexedNotNull { index, (constellation, snrList) ->
                        val clipped = if (snrList.size > CHART_UPDATE_WINDOW) snrList.takeLast(
                            CHART_UPDATE_WINDOW
                        ) else snrList

                        if (clipped.isEmpty()) return@mapIndexedNotNull null // skip empty series

                        val startX = (CHART_UPDATE_WINDOW - clipped.size).coerceAtLeast(0)
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
                lineChart.setVisibleXRangeMaximum(CHART_UPDATE_WINDOW.toFloat())
                lineChart.invalidate() // Refresh the chart
            }
        )
    }
}

@Composable
fun SNRChartCard(
    snrHistory: List<Float>,
    constellation: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(160.dp)
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        AndroidView(
            modifier = modifier
                .fillMaxSize()
                .padding(10.dp),
            factory = { context -> applyChartSettings(context) },
            update = update@{ lineChart ->
                val clipped = if (snrHistory.size > CHART_UPDATE_WINDOW)
                    snrHistory.takeLast(CHART_UPDATE_WINDOW)
                    else snrHistory

                val startX = (CHART_UPDATE_WINDOW - clipped.size).coerceAtLeast(0)
                val entries = clipped.mapIndexedNotNull { i, snr ->
                    if (snr != 0f) Entry(startX + i.toFloat(), snr) else null
                }

                val dataSet = LineDataSet(entries, constellation.ifBlank { "SNR" }).apply {
                    color = getConstellationColor(constellation = constellation).toArgb()
                    setDrawCircles(false)
                    lineWidth = 2f
                    setDrawValues(false)
                    mode = LineDataSet.Mode.LINEAR
                }

                lineChart.data = LineData(dataSet)
                lineChart.setVisibleXRangeMaximum(CHART_UPDATE_WINDOW.toFloat())
                lineChart.invalidate() // Refresh chart
            }
        )
    }
}

private fun applyChartSettings(context: Context): LineChart {
    return LineChart(context).apply {
        description.isEnabled = false
        axisRight.isEnabled = false
        setTouchEnabled(true)
        setPinchZoom(true)

        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            granularity = 1f
            isGranularityEnabled = true
            axisMinimum = 0f
            axisMaximum = CHART_UPDATE_WINDOW.toFloat()
            textColor = TextPrimary.toArgb()
            textSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val secondsAgo = (CHART_UPDATE_WINDOW - value.toInt()) * 1 // 1 second per entry
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
        else -> Color.Companion.LightGray
    }
}