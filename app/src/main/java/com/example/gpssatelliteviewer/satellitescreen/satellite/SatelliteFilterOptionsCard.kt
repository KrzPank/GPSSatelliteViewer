package com.example.gpssatelliteviewer.satellitescreen.satellite

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpssatelliteviewer.utils.CustomCheckbox

@Composable
fun FilterOptionsCard(
    showFilterDialog: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.Companion
            .fillMaxWidth()
            .clickable { onToggle(!showFilterDialog) }
    ) {
        Row(
            modifier = Modifier.Companion
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Text(
                text = "Filter options",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 20.sp,
            )
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Filter constellations / satellites",
                modifier = Modifier.Companion.size(24.dp)
            )
        }
    }
}


@Composable
fun FilterDialog(
    showOnlyInFix: Boolean,
    onShowOnlyInFixChange: (Boolean) -> Unit,
    constellations: List<String>,
    selectedConstellations: Set<String>,
    onConstellationsChange: (Set<String>) -> Unit,
    satellites: Set<String>,
    selectedSatellites: Set<String>,
    onSatellitesChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Filter Satellites") },
        text = {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                CustomCheckbox(
                    label = "Show only used in Fix",
                    checked = showOnlyInFix,
                    onCheckedChange = onShowOnlyInFixChange
                )

                HorizontalDivider()

                // Constellation selection
                Text(
                    text = "Filter by Constellation",
                    style = MaterialTheme.typography.titleMedium
                )
                constellations.forEach { constellation ->
                    CustomCheckbox(
                        label = constellation,
                        checked = constellation in selectedConstellations,
                        onCheckedChange = {
                            onConstellationsChange(
                                selectedConstellations.toggle(constellation)
                            )
                        }
                    )
                }

                HorizontalDivider()

                Text("Select Satellites", style = MaterialTheme.typography.titleMedium)

                val filteredSatellites = satellites.filter { satKey ->
                    val constellationPrefix = satKey.substringBefore(":")
                    selectedConstellations.isEmpty() || selectedConstellations.contains(
                        constellationPrefix
                    )
                }

                filteredSatellites.forEach { satKey ->
                    CustomCheckbox(
                        label = satKey,
                        checked = satKey in selectedSatellites,
                        onCheckedChange = {
                            onSatellitesChange(
                                selectedSatellites.toggle(satKey)
                            )
                        }
                    )
                }
            }
        }
    )
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (contains(item)) this - item else this + item
