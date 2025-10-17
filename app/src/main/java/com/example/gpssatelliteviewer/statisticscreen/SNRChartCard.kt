package com.example.gpssatelliteviewer.statisticscreen

import android.content.Context
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.gpssatelliteviewer.app.theme.TextHint
import com.example.gpssatelliteviewer.app.theme.TextPrimary
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
fun ConstellationSNRChartCard(
    snrHistory: Map<String, List<Float>>,
    modifier: Modifier = Modifier
) {
    val meaningfulSnrHistory = snrHistory.filterValues { list ->
        list.any { it != 0f }
    }

    val allConstellations = meaningfulSnrHistory.keys.toList()
    var selectedConstellations by remember { mutableStateOf(allConstellations.toSet()) }
    var previousConstellations by remember { mutableStateOf(allConstellations.toSet()) }

    LaunchedEffect(allConstellations) {
        val newOnes = allConstellations.filterNot { it in previousConstellations }
        previousConstellations = allConstellations.toSet()

        if (newOnes.isNotEmpty()) {
            selectedConstellations = selectedConstellations + newOnes
        }
    }

    // --- Chart Card ---
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
        ) {
            /*
            Text(
                text = "AVG. SNR in Fix",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 20.sp,
            )

             */
            GroupedSNRChart(
                snrHistory = meaningfulSnrHistory,
                selectedConstellations = selectedConstellations,
                modifier = modifier
            )
        }
    }

    // --- Filter Chips ---
    if (allConstellations.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            allConstellations.forEach { constellation ->
                val isSelected = constellation in selectedConstellations
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedConstellations = if (isSelected) {
                            selectedConstellations - constellation
                        } else {
                            selectedConstellations + constellation
                        }
                    },
                    label = { Text(constellation) },
                    leadingIcon = if (isSelected) {
                        { Icon(
                            Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = TextHint,
                            modifier = Modifier.size(18.dp))
                        }
                    } else {
                        { Icon(Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = TextHint,
                            modifier = Modifier.size(18.dp))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GroupedSNRChart(
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
fun ConstellationSNRChartCard(
    snrHistory: List<Float>,
    constellation: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
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
                        // gap -> flush current segment if any
                        if (currentEntries.isNotEmpty()) {
                            val ds = LineDataSet(currentEntries, constellation).apply {
                                color = getConstellationColor(constellation).toArgb()
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

                // flush last segment
                if (currentEntries.isNotEmpty()) {
                    val ds = LineDataSet(currentEntries, constellation).apply {
                        color = getConstellationColor(constellation).toArgb()
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
        legend.textColor = TextPrimary.toArgb()
        legend.textSize = 14f

        val distinctLabelColor = datasets
            .map { ds -> ds.label to ds.color }
            .distinctBy { it.first }              // keep one entry per label

        // Convert to LegendEntry list (set color and label)
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