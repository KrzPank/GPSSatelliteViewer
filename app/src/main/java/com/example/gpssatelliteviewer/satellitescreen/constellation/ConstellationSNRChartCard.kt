package com.example.gpssatelliteviewer.satellitescreen.constellation

import android.widget.Space
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpssatelliteviewer.app.theme.TextHintColor
import com.example.gpssatelliteviewer.data.TimestampedSNR
import com.example.gpssatelliteviewer.satellitescreen.GroupedSNRChart
import com.example.gpssatelliteviewer.satellitescreen.IndividualSNRChart
import com.example.gpssatelliteviewer.satellitescreen.getConstellationColor
import kotlin.collections.plus

@Composable
fun GroupedConstellationSNRChartCard(
    snrHistory: Map<String, List<TimestampedSNR>>,
    label: String,
    modifier: Modifier = Modifier
) {
    val meaningfulSnrHistory = snrHistory.filterValues { list ->
        list.any { it.snr != 0f }
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
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(5.dp))
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
                            tint = TextHintColor,
                            modifier = Modifier.size(18.dp))
                        }
                    } else {
                        { Icon(Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = TextHintColor,
                            modifier = Modifier.size(18.dp))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun IndividualConstellationSNRChartCard(
    constellationSNRHistory: List<TimestampedSNR>,
    constellation: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.Companion
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        IndividualSNRChart(
            snrHistory = constellationSNRHistory,
            lineColor = getConstellationColor(constellation),
            label = constellation,
            modifier = modifier
        )
    }
}
