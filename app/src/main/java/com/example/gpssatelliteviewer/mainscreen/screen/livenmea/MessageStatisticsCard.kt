package com.example.gpssatelliteviewer.mainscreen.screen.livenmea

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.utils.InfoRow

@Composable
fun MessageStatisticsCard(
    statistics: Map<String, Int>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    standardOrder: List<String> = listOf("GGA", "RMC", "GSA", "VTG"),
    gsvOrder: List<String> = listOf("GPGSV", "GLGSV", "GBGSV", "GAGSV", "GQGSV"),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            )
    ) {
        Column(
            modifier = modifier
                .padding(12.dp)) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!isExpanded) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Message Statistics",
                    style = MaterialTheme.typography.titleMedium,
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessHigh
                    )
                )
            ) {
                Column {
                    Spacer(
                        modifier = modifier
                            .height(6.dp))

                    val totalMessages = statistics.values.sum()

                    InfoRow("Total messages received", totalMessages.toString())

                    // Sort statistics: Standard messages first, then GSV in order, then vendor alphabetically
                    val standardMessages = standardOrder.mapNotNull { key ->
                        statistics[key]?.let { count -> key to count }
                    }

                    val gsvMessages = gsvOrder.mapNotNull { key ->
                        statistics[key]?.let { count -> key to count }
                    }

                    val vendorMessages = statistics
                        .filterKeys { it !in standardOrder && it !in gsvOrder }
                        .toList()
                        .sortedBy { it.first }

                    val sortedEntries = standardMessages + gsvMessages + vendorMessages

                    sortedEntries.forEach { (messageType, count) ->
                        InfoRow(messageType, count.toString())
                    }

                    if (sortedEntries.isEmpty()) {
                        InfoRow("No messages", "received yet")
                    }
                }
            }
        }
    }
}