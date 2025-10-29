package com.example.gpssatelliteviewer.satellitescreen

import android.content.Context
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gpssatelliteviewer.app.theme.ChartBeoDouColor
import com.example.gpssatelliteviewer.app.theme.ChartGLONASSColor
import com.example.gpssatelliteviewer.app.theme.ChartGPSColor
import com.example.gpssatelliteviewer.app.theme.ChartGalileoColor
import com.example.gpssatelliteviewer.app.theme.ChartIRNSSColor
import com.example.gpssatelliteviewer.app.theme.ChartQZSSColor
import com.example.gpssatelliteviewer.app.theme.ChartSBASColor
import com.example.gpssatelliteviewer.app.theme.TextPrimaryColor
import com.example.gpssatelliteviewer.data.CHART_UPDATE_WINDOW
import com.example.gpssatelliteviewer.data.TimestampedSNR
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.sql.Date
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2

class TimeAxisFormatter(
    var lastTimestampMillis: Long = System.currentTimeMillis()
) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val secondsAgo = (CHART_UPDATE_WINDOW - value.toInt()) * 1 // 1 second per entry
        val timestampMillis = System.currentTimeMillis() - secondsAgo * 1000
        val date = Date(timestampMillis)
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return if (value % 10 == 0f) sdf.format(date) else ""
    }
}

@Composable
fun GroupedSNRChart(
    snrHistory: Map<String, List<TimestampedSNR>>,
    selectedConstellations: Set<String>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxSize(),
        factory = { context -> applyChartSettings(context) },
        update = update@{ lineChart ->
            val filteredSnr = snrHistory.filterKeys { it in selectedConstellations }
            val dataSets = mutableListOf<ILineDataSet>()

            var overallLastTs: Long? = null

            filteredSnr.values.forEach { list ->
                if (list.isNotEmpty()) {
                    val lastTs = list.last().timestamp
                    overallLastTs = maxOf(overallLastTs ?: lastTs, lastTs)
                }
            }

            if (overallLastTs == null) {
                lineChart.data = LineData()
                applyDistinctLegend(lineChart, emptyList())
                lineChart.invalidate()
                return@update
            }

            val lastTs = overallLastTs

            filteredSnr.forEach { (constellation, snrList) ->
                val clipped = if (snrList.size > CHART_UPDATE_WINDOW)
                    snrList.takeLast(CHART_UPDATE_WINDOW)
                else snrList

                if (clipped.isEmpty()) return@forEach

                var currentEntries = mutableListOf<Entry>()
                clipped.forEach { tsSnr ->
                    // map timestamp -> x where newest sample (lastTs) -> CHART_UPDATE_WINDOW - 1
                    val secondsDiff = (lastTs - tsSnr.timestamp) / 1000f
                    var x = (CHART_UPDATE_WINDOW - 1) - secondsDiff
                    x = x.coerceIn(0f, (CHART_UPDATE_WINDOW - 1).toFloat())
                    val y = tsSnr.snr

                    if (y == 0f) {
                        if (currentEntries.isNotEmpty()) {
                            val ds = LineDataSet(currentEntries, constellation).apply {
                                color = getConstellationColor(constellation).toArgb()
                                setDrawCircles(false)
                                lineWidth = 2f
                                setDrawValues(false)
                                mode = LineDataSet.Mode.LINEAR
                                isHighlightEnabled = false
                            }
                            dataSets.add(ds)
                            currentEntries = mutableListOf()
                        }
                    } else {
                        currentEntries.add(Entry(x, y))
                    }
                }

                if (currentEntries.isNotEmpty()) {
                    val ds = LineDataSet(currentEntries, constellation).apply {
                        color = getConstellationColor(constellation).toArgb()
                        setDrawCircles(false)
                        lineWidth = 2f
                        setDrawValues(false)
                        mode = LineDataSet.Mode.LINEAR
                        isHighlightEnabled = false
                    }
                    dataSets.add(ds)
                }
            }
            (lineChart.xAxis.valueFormatter as? TimeAxisFormatter)?.lastTimestampMillis = lastTs

            lineChart.data = LineData(dataSets)
            applyDistinctLegend(lineChart, dataSets)
            lineChart.setVisibleXRangeMaximum((CHART_UPDATE_WINDOW - 1).toFloat())
            lineChart.notifyDataSetChanged()
            lineChart.invalidate()
        }
    )
}

@Composable
fun IndividualSNRChart(
    snrHistory: List<TimestampedSNR>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        factory = { context ->
            applyChartSettings(context)
        },
        update = update@{ lineChart ->
            // ensure chronological order then clip to window
            val sorted = snrHistory.sortedBy { it.timestamp }
            val clipped = if (sorted.size > CHART_UPDATE_WINDOW)
                sorted.takeLast(CHART_UPDATE_WINDOW)
            else sorted

            if (clipped.isEmpty()) {
                lineChart.data = LineData()
                applyDistinctLegend(lineChart, emptyList())
                lineChart.invalidate()
                return@update
            }

            val lastTs = clipped.last().timestamp

            val datasets = mutableListOf<LineDataSet>()
            var currentEntries = mutableListOf<Entry>()

            clipped.forEach { tsSnr ->
                // Map timestamp -> x where newest sample (lastTs) -> CHART_UPDATE_WINDOW - 1
                val secondsDiff = (lastTs - tsSnr.timestamp) / 1000f
                var x = (CHART_UPDATE_WINDOW - 1) - secondsDiff
                x = x.coerceIn(0f, (CHART_UPDATE_WINDOW - 1).toFloat())
                val y = tsSnr.snr

                if (y == 0f) {
                    if (currentEntries.isNotEmpty()) {
                        val ds = LineDataSet(currentEntries, label).apply {
                            color = lineColor.toArgb()
                            setDrawCircles(false)
                            lineWidth = 2f
                            setDrawValues(false)
                            mode = LineDataSet.Mode.LINEAR
                            isHighlightEnabled = false
                        }
                        datasets.add(ds)
                        currentEntries = mutableListOf()
                    }
                } else {
                    currentEntries.add(Entry(x, y))
                }
            }

            if (currentEntries.isNotEmpty()) {
                val ds = LineDataSet(currentEntries, label).apply {
                    color = lineColor.toArgb()
                    setDrawCircles(false)
                    lineWidth = 2f
                    setDrawValues(false)
                    mode = LineDataSet.Mode.LINEAR
                    isHighlightEnabled = false
                }
                datasets.add(ds)
            }

            (lineChart.xAxis.valueFormatter as? TimeAxisFormatter)?.lastTimestampMillis = lastTs

            lineChart.data = LineData(datasets as List<ILineDataSet>)
            applyDistinctLegend(lineChart, datasets)
            lineChart.legend.isEnabled = label != null

            lineChart.setVisibleXRangeMaximum((CHART_UPDATE_WINDOW - 1).toFloat())
            lineChart.notifyDataSetChanged()
            lineChart.invalidate()
        }
    )
}

private fun applyDistinctLegend(
    lineChart: LineChart,
    datasets: List<ILineDataSet>,
) {
    val legend = lineChart.legend
    if (datasets.isEmpty()) {
        legend.resetCustom()
        legend.isEnabled = false
    } else {
        legend.isEnabled = true
        legend.form = Legend.LegendForm.LINE
        legend.textColor = TextPrimaryColor.toArgb()
        legend.textSize = 14f

        val distinctLabelColor = datasets
            .map { ds -> ds.label to ds.color }
            .distinctBy { it.first }              // keep one entry per label

        val legendEntries = distinctLabelColor.map { (lbl, color) ->
            LegendEntry().apply {
                label = lbl
                form = Legend.LegendForm.LINE
                formColor = color
            }
        }
        legend.setCustom(legendEntries)
    }
}

private fun applyChartSettings(context: Context): LineChart {
    return LineChart(context).apply {
        description.isEnabled = false
        axisLeft.isEnabled = false
        setTouchEnabled(true)
        setPinchZoom(true)

        axisRight.apply {
            isEnabled = true
            setDrawGridLines(true)
            textColor = TextPrimaryColor.toArgb()
            textSize = 10f
        }

        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(true)
            granularity = 1f
            isGranularityEnabled = true
            axisMinimum = 0f
            axisMaximum = (CHART_UPDATE_WINDOW - 1).toFloat()
            textColor = TextPrimaryColor.toArgb()
            textSize = 10f

            // install a TimeAxisFormatter instance that will be updated by chart update blocks
            valueFormatter = TimeAxisFormatter(System.currentTimeMillis())
        }

        legend.apply {
            isEnabled = true
            form = Legend.LegendForm.LINE
            textColor = TextPrimaryColor.toArgb()
            textSize = 14f
        }
    }
}

fun getConstellationColor(constellation: String): Color {
    return when (constellation.uppercase()) {
        "GPS" -> ChartGPSColor
        "GLONASS" -> ChartGLONASSColor
        "GALILEO" -> ChartGalileoColor
        "BEIDOU" -> ChartBeoDouColor
        "QZSS" -> ChartQZSSColor
        "IRNSS" -> ChartIRNSSColor
        "SBAS" -> ChartSBASColor
        else -> Color.Companion.LightGray
    }
}
