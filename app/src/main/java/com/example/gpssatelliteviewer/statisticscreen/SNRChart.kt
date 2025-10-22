package com.example.gpssatelliteviewer.statisticscreen

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

@Composable
fun GroupedSNRChart(
    snrHistory: Map<String, List<Float>>,
    selectedConstellations: Set<String>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxSize(),
        factory = { context -> applyChartSettings(context) },
        update = { lineChart ->
            // Filter only selected constellations
            val filteredSnr = snrHistory.filterKeys { it in selectedConstellations }

            val dataSets = filteredSnr.entries.flatMap { (constellation, snrList) ->
                val clipped = if (snrList.size > CHART_UPDATE_WINDOW)
                    snrList.takeLast(CHART_UPDATE_WINDOW)
                else snrList

                if (clipped.isEmpty()) return@flatMap emptyList<ILineDataSet>()

                val startX = (CHART_UPDATE_WINDOW - clipped.size).coerceAtLeast(0)

                val dataset = mutableListOf<LineDataSet>()
                var currentEntries = mutableListOf<Entry>()

                clipped.forEachIndexed { i, snr ->
                    val x = (startX + i).toFloat()
                    if (snr == 0f) {
                        if (currentEntries.isNotEmpty()) {
                            val ds = LineDataSet(currentEntries, constellation).apply {
                                color = getConstellationColor(constellation).toArgb()
                                setDrawCircles(false)
                                lineWidth = 2f
                                setDrawValues(false)
                                mode = LineDataSet.Mode.LINEAR
                                isHighlightEnabled = false
                            }
                            dataset.add(ds)
                            currentEntries = mutableListOf()
                        }
                    } else {
                        currentEntries.add(Entry(x, snr))
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
                    dataset.add(ds)
                }

                dataset
            }

            lineChart.data = LineData(dataSets)
            applyDistinctLegend(lineChart, dataSets)
            lineChart.setVisibleXRangeMaximum(CHART_UPDATE_WINDOW.toFloat())
            lineChart.notifyDataSetChanged()
            lineChart.invalidate()
        }
    )
}

@Composable
fun IndividualSNRChart(
    snrHistory: List<Float>,
    lineColor: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        factory = { context ->
            applyChartSettings(context)
        },
        update = update@{ lineChart ->
            val clipped = if (snrHistory.size > CHART_UPDATE_WINDOW)
                snrHistory.takeLast(CHART_UPDATE_WINDOW)
            else snrHistory

            val startX = (CHART_UPDATE_WINDOW - clipped.size).coerceAtLeast(0)

            // Build contiguous segments: each non-zero run becomes its own dataset
            val datasets = mutableListOf<LineDataSet>()
            var currentEntries = mutableListOf<Entry>()

            clipped.forEachIndexed { i, snr ->
                val x = (startX + i).toFloat()
                if (snr == 0f) {
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
                    currentEntries.add(Entry(x, snr))
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

            lineChart.data = LineData(datasets as List<ILineDataSet>)
            applyDistinctLegend(lineChart, datasets)

            lineChart.setVisibleXRangeMaximum(CHART_UPDATE_WINDOW.toFloat())
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
            axisMaximum = CHART_UPDATE_WINDOW.toFloat()
            textColor = TextPrimaryColor.toArgb()
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
