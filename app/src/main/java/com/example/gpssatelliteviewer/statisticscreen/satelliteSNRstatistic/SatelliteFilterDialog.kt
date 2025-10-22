package com.example.gpssatelliteviewer.statisticscreen.satelliteSNRstatistic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.utils.CustomCheckbox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteSNRFilterDialog(
    allConstellations: List<String>,
    validKeys: List<String>,
    selectedConstellations: Set<String>,
    onSelectedConstellationsChange: (Set<String>) -> Unit,
    selectedSatelliteKeys: Set<String>,
    onSelectedSatelliteKeysChange: (Set<String>) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleSatelliteKeys = remember(validKeys, selectedConstellations) {
        validKeys.filter { key ->
            val constellation = key.substringBefore(":").trim()
            selectedConstellations.contains(constellation)
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onSelectedConstellationsChange(allConstellations.toSet())
                onSelectedSatelliteKeysChange(emptySet())
            }) {
                Text("Reset")
            }
        },
        title = { Text("Constellation & Satellite filters") },
        text = {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    //.heightIn(min = 500.dp ,max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // === CONSTELLATIONS ===
                Text(
                    text = "Constellations",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onSelectedConstellationsChange(allConstellations.toSet()) }) {
                        Text("Select All")
                    }
                    TextButton(onClick = {
                        onSelectedConstellationsChange(emptySet())
                        onSelectedSatelliteKeysChange(emptySet())
                    }) {
                        Text("Clear")
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    allConstellations.forEach { constellation ->
                        val checked = selectedConstellations.contains(constellation)

                        CustomCheckbox(
                            label = constellation,
                            checked = checked,
                            onCheckedChange = { checkedNow ->
                                val newSet = selectedConstellations.toMutableSet().apply {
                                    if (checkedNow) add(constellation) else remove(constellation)
                                }.toSet()

                                onSelectedConstellationsChange(newSet)

                                // Ensure satellite keys match only selected constellations
                                onSelectedSatelliteKeysChange(
                                    selectedSatelliteKeys.filter { key ->
                                        val c = key.substringBefore(":").trim()
                                        newSet.contains(c)
                                    }.toSet()
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // === SATELLITES ===
                Text(
                    text = "Select individual satellites from selected constellations",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onSelectedSatelliteKeysChange(visibleSatelliteKeys.toSet()) }) {
                        Text("Select All Visible")
                    }
                    TextButton(onClick = { onSelectedSatelliteKeysChange(emptySet()) }) {
                        Text("Clear")
                    }
                }

                if (visibleSatelliteKeys.isEmpty()) {
                    Text(
                        text = "No satellites available for the selected constellations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        visibleSatelliteKeys.forEach { key ->
                            val checked = selectedSatelliteKeys.contains(key)

                            CustomCheckbox(
                                label = key,
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    val newSet = selectedSatelliteKeys.toMutableSet().apply {
                                        if (isChecked) add(key) else remove(key)
                                    }.toSet()
                                    onSelectedSatelliteKeysChange(newSet)
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}
