package com.example.gpssatelliteviewer.ui.screen


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController

// GOLD NEVER FORGET
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.ui.component.card.NMEAMessageCard
import com.example.gpssatelliteviewer.ui.component.card.MessageStatisticsCard
import com.example.gpssatelliteviewer.data.viewmodel.NMEAViewModel
import com.example.gpssatelliteviewer.data.NMEAMessage
import com.example.gpssatelliteviewer.ui.component.card.RenderGSVInfo


// New version using sealed classes - cleaner and more type-safe
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun LiveNMEADataScreen(
    navController: NavController,
    viewModel: NMEAViewModel
) {
    val latestMessages by viewModel.latestMessages.collectAsState()
    val nmeaMessageMap by viewModel.nmeaMessageMap.collectAsState()
    val messageStatistics by viewModel.messageStatistics.collectAsState()

    var dropDownMenuExpanded = remember { mutableStateOf(false) }
    var statisticsExpanded = remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Live NMEA Messages", style = MaterialTheme.typography.titleLarge)
                },
                actions = {
                    Box {
                        IconButton(onClick = { dropDownMenuExpanded.value = true}) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                        DropdownMenu(
                            expanded = dropDownMenuExpanded.value,
                            onDismissRequest = { dropDownMenuExpanded.value = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Satellite 3D View") },
                                onClick = {
                                    dropDownMenuExpanded.value = false
                                    navController.navigate("Satellite3DScreen")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Location Info") },
                                onClick = {
                                    dropDownMenuExpanded.value = false
                                    navController.navigate("LocationInfoScreen")
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                // Show message statistics first
                if (messageStatistics.isNotEmpty()) {
                    item(key = "statistics") {
                        MessageStatisticsCard(
                            statistics = messageStatistics,
                            isExpanded = statisticsExpanded.value,
                            onExpandedChange = { statisticsExpanded.value = it }
                        )
                    }
                }
                
                // Define the order for standard NMEA messages
                val standardOrder = listOf("GGA", "RMC", "GSA", "VTG")
                val gsvMessages = latestMessages.filter { it.key.contains("GSV") }
                val standardMessages = latestMessages.filter { it.key in standardOrder }
                val vendorMessages = latestMessages.filter { it.key !in standardOrder && !it.key.contains("GSV") }
                
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
            }
        }
    }
}
