package com.example.gpssatelliteviewer.statisticscreen.satelliteSNRstatistic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteSNRStatisticScreen (
    gnssViewModel: GNSSViewModel
) {
    val satelliteSNRHistory by gnssViewModel.satelliteSNRHistory.collectAsState()
    val keys = satelliteSNRHistory.keys.toList()

    val validKeys = keys.filter { key ->
        val snrList = satelliteSNRHistory[key]
        !snrList.isNullOrEmpty() && snrList.any() { it != 0f}
    }

    val allConstellations = validKeys
        .map { it.substringBefore(":").trim() }
        .distinct()
        .sorted()

    var selectedConstellations by remember { mutableStateOf(allConstellations.toSet()) }
    var previousConstellations by remember { mutableStateOf(allConstellations.toSet()) }

    LaunchedEffect(allConstellations) {
        val newOnes = allConstellations.filterNot { it in previousConstellations }
        previousConstellations = allConstellations.toSet()
        if (newOnes.isNotEmpty()) {
            selectedConstellations = selectedConstellations + newOnes
        }
    }

    var showFilterDialog by remember { mutableStateOf(false) }

    var selectedSatelliteKeys by remember { mutableStateOf(setOf<String>()) }

    val filteredKeys = remember(validKeys, selectedConstellations, selectedSatelliteKeys) {
        if (selectedSatelliteKeys.isNotEmpty()) {
            validKeys.filter { it in selectedSatelliteKeys && it in validKeys}
        } else {
            validKeys.filter { key ->
                val constellation = key.substringBefore(":").trim()
                selectedConstellations.contains(constellation)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.Companion
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(
                modifier = Modifier.Companion
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SNR for individual satellites",
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 20.sp,
                )
                IconButton(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Filter constellations / satellites",
                        //modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        items(filteredKeys) { key ->
            SatelliteSNRChartCard(
                satelliteSNRHistory = satelliteSNRHistory[key]!!,
                key = key,
                modifier = Modifier.Companion.height(150.dp)
            )
        }
    }

    if (showFilterDialog) {
        SatelliteSNRFilterDialog(
            allConstellations = allConstellations,
            validKeys = validKeys,
            selectedConstellations = selectedConstellations,
            onSelectedConstellationsChange = { newSet -> selectedConstellations = newSet },
            selectedSatelliteKeys = selectedSatelliteKeys,
            onSelectedSatelliteKeysChange = { newSet -> selectedSatelliteKeys = newSet },
            onDismissRequest = { showFilterDialog = false }
        )
    }
}
