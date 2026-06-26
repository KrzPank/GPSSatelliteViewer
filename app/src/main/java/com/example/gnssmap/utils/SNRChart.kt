package com.example.gnssmap.utils

import android.content.Context
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gnssmap.app.theme.TextPrimaryColor
import com.example.gnssmap.data.CHART_UPDATE_WINDOW
import com.example.gnssmap.data.TimestampedData
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
fun GroupedChart(
    dataHistory: Map<String, List<TimestampedData>>,
    selectedConstellations: Set<String>,
    modifier: Modifier = Modifier
) {
    // Optimize Data Processing: Only recalculate LineData when history or selections change
    val lineDataAndLastTs by remember(dataHistory, selectedConstellations) {
        derivedStateOf {
            val filteredSnr = dataHistory.filterKeys { it in selectedConstellations }
            val dataSets = mutableListOf<ILineDataSet>()
            var overallLastTs: Long? = null

            // Find the latest timestamp
            filteredSnr.values.forEach { list ->
                if (list.isNotEmpty()) {
                    val lastTs = list.last().timestamp
                    overallLastTs = maxOf(overallLastTs ?: lastTs, lastTs)
                }
            }

            if (overallLastTs == null) {
                return@derivedStateOf Pair(LineData(), null)
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
                    val y = tsSnr.data

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
            Pair(LineData(dataSets), lastTs)
        }
    }

    // just pushes pre-calculated data into the view
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context -> applyChartSettings(context) },
        update = { lineChart ->
            val (lineData, lastTs) = lineDataAndLastTs

            if (lastTs == null) {
                lineChart.data = LineData()
                applyDistinctLegend(lineChart, emptyList())
                lineChart.invalidate()
                return@AndroidView
            }

            // Update time formatter reference
            (lineChart.xAxis.valueFormatter as? TimeAxisFormatter)?.lastTimestampMillis = lastTs

            lineChart.data = lineData
            applyDistinctLegend(lineChart, lineData.dataSets)
            lineChart.setVisibleXRangeMaximum((CHART_UPDATE_WINDOW - 1).toFloat())
            lineChart.notifyDataSetChanged()
            lineChart.invalidate()
        }
    )
}

@Composable
fun IndividualChart(
    dataHistory: List<TimestampedData>,
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
            val sorted = dataHistory.sortedBy { it.timestamp }
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

            clipped.forEach { tsData ->
                // Map timestamp -> x where newest sample (lastTs) -> CHART_UPDATE_WINDOW - 1
                val secondsDiff = (lastTs - tsData.timestamp) / 1000f
                var x = (CHART_UPDATE_WINDOW - 1) - secondsDiff
                x = x.coerceIn(0f, (CHART_UPDATE_WINDOW - 1).toFloat())
                val y = tsData.data

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