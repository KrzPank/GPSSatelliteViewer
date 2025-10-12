package com.example.gpssatelliteviewer.mainscreen.screen.livenmea

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularNodata
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.data.viewmodel.NMEAViewModel

// GOLD NEVER FORGET
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.gpssatelliteviewer.statisticscreen.MessageStatisticsCard
import com.example.gpssatelliteviewer.utils.EmptyStateCard

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun LiveNMEADataScreen(
    viewModel: NMEAViewModel
) {
    val latestMessages by viewModel.latestMessages.collectAsState()
    val nmeaMessageMap by viewModel.nmeaMessageMap.collectAsState()
    val messageStatistics by viewModel.messageStatistics.collectAsState()

    var statisticsExpanded = remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.Companion
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.Companion
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            if (messageStatistics.isNotEmpty()) {
                item {
                    MessageStatisticsCard(
                        statistics = messageStatistics,
                        isExpanded = statisticsExpanded.value,
                        onExpandedChange = { statisticsExpanded.value = it }
                    )
                }
                // Define the order for standard NMEA messages
                val standardOrder = listOf("GGA", "RMC", "GSA", "VTG")
                val gsvMessages = latestMessages.filter { it.key.contains("GSV") }
                val standardMessages = latestMessages.filter { it.key in standardOrder }
                val vendorMessages =
                    latestMessages.filter { it.key !in standardOrder && !it.key.contains("GSV") }

                // Show standard NMEA messages in defined order
                items(
                    items = standardOrder.mapNotNull { key ->
                        standardMessages[key]?.let { message -> key to message }
                    },
                    key = { it.first }
                ) { (type, message) ->
                    NMEAMessageCard(
                        message = message,
                        rawMessage = nmeaMessageMap[type] ?: ""
                    )
                }

                // Show GSV messages as a group if any exist
                if (gsvMessages.isNotEmpty()) {
                    item(key = "gsv_group") {
                        RenderGSVInfo(gsvMessages = gsvMessages)
                    }
                }

                // Show vendor/unknown messages last
                items(
                    items = vendorMessages.entries.toList(),
                    key = { it.key }
                ) { (type, message) ->
                    NMEAMessageCard(
                        message = message,
                        rawMessage = nmeaMessageMap[type] ?: ""
                    )
                }
            } else {
                item {
                    EmptyStateCard(
                        message = "No NMEA data received yet.",
                        icon = Icons.Default.SignalCellularNodata
                    )
                }
            }
        }
    }
}