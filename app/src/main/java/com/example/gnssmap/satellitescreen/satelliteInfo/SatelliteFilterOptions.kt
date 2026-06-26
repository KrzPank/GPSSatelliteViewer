package com.example.gnssmap.satellitescreen.satelliteInfo

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gnssmap.app.theme.DeselectAllButtonColor
import com.example.gnssmap.data.GNSSStatusData
import com.example.gnssmap.utils.CustomCheckbox

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
    satellites: List<GNSSStatusData>,
    selectedConstellations: Set<String>,
    onConstellationsChange: (Set<String>) -> Unit,
    selectedSatellites: Set<String>,
    onSatellitesChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val constellations = remember(satellites) { satellites.map { it.constellation }.distinct() }
    val allSatellites = remember(satellites) { satellites.map { "${it.constellation}:${it.prn}"} }
    val usedInFixKeys = remember(satellites) {
        satellites
            .asSequence()
            .filter { it.usedInFix == true}
            .map { "${it.constellation}:${it.prn}" }
            .toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = {
                onShowOnlyInFixChange(false)
                onConstellationsChange(emptySet())
                onSatellitesChange(emptySet())
            }) {
                Text(
                    text = "Reset selection",
                    color = DeselectAllButtonColor
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Filter Satellites") },
        text = {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider()

                CustomCheckbox(
                    label = "Show only used in Fix",
                    checked = showOnlyInFix,
                    onCheckedChange = onShowOnlyInFixChange
                )

                // Constellation selection
                Text(
                    text = "Filter by Constellation",
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider()
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

                Text("Select Satellites", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()

                val filteredSatellites = remember(
                    allSatellites,
                    usedInFixKeys,
                    selectedConstellations,
                    showOnlyInFix
                ) {
                    allSatellites.filter { satKey ->
                        val constellationPrefix = satKey.substringBefore(":")
                        val matchesConstellation = selectedConstellations.isEmpty() ||
                                selectedConstellations.contains(constellationPrefix)

                        if (showOnlyInFix) {
                            matchesConstellation && (satKey in usedInFixKeys)
                        } else {
                            matchesConstellation
                        }
                    }
                }.sortedWith(compareBy(
                    { it.substringBefore(":") },
                    { it.substringAfter(":").toIntOrNull() ?: Int.MAX_VALUE }
                ))

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
