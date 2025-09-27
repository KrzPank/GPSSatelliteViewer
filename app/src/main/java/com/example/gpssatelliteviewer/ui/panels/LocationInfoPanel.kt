package com.example.gpssatelliteviewer.ui.panels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gpssatelliteviewer.ui.cards.AndroidApiLocationCard
import com.example.gpssatelliteviewer.ui.cards.LoadingLocationTextCard
import com.example.gpssatelliteviewer.ui.cards.NMEALocationCard
import com.example.gpssatelliteviewer.ui.cards.GPSStatusCard


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.example.gpssatelliteviewer.utils.InfoRow
import com.example.gpssatelliteviewer.viewModel.GNSSViewModel
import com.example.gpssatelliteviewer.viewModel.LocationListenerViewModel
import com.example.gpssatelliteviewer.viewModel.NMEAViewModel

//*
@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationInfoPanel(
    navController: NavController,
    gnssStatusViewModel: GNSSViewModel,
    nmeaViewModel: NMEAViewModel,
    locationListenerViewModel: LocationListenerViewModel
) {
    val satellites by gnssStatusViewModel.satelliteList.collectAsState()
    val groupedSatellites = satellites.groupBy { it.constellation }

    val locationNMEA by nmeaViewModel.locationNMEA.collectAsState()
    val locationAndroidApi by locationListenerViewModel.locationAndroidApi.collectAsState()

    val hasLocationNMEA by nmeaViewModel.hasLocationNMEA.collectAsState()
    val hasLocationAndroidApi by locationListenerViewModel.hasLocationAndroidApi.collectAsState()

    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    var dropDownMenuExpanded = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location Info", style = MaterialTheme.typography.titleLarge) },
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
                                text = { Text("3D View") },
                                onClick = {
                                    dropDownMenuExpanded.value = false
                                    navController.navigate("Satellite3DPanel")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("NMEA messaegs") },
                                onClick = {
                                    dropDownMenuExpanded.value = false
                                    navController.navigate("LiveNMEADataPanel")
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- Location Card ---
            item {
                when {
                    hasLocationNMEA -> NMEALocationCard(locationNMEA)
                    hasLocationAndroidApi -> AndroidApiLocationCard(locationAndroidApi)
                    else -> LoadingLocationTextCard()
                }
            }

            // --- Summary of satellites ---
            item {
                GPSStatusCard(satellites)
            }

            // --- Satellites grouped by constellation ---
            item {
                Text(
                    "Satellites by Constellation",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                )
            }

            groupedSatellites.forEach { (constellation, satellitesInGroup) ->
                val expanded = expandedMap.getOrPut(constellation) { false }

                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedMap[constellation] = !expanded },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$constellation (${satellitesInGroup.size})",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null
                            )
                        }
                    }
                }

                if (expanded) {
                    items(satellitesInGroup) { satellite ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 1.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                InfoRow("PRN", satellite.prn.toString())
                                InfoRow("SNR", "${satellite.snr} dBHz")
                                InfoRow("Used in Fix", satellite.usedInFix.toString())
                                InfoRow("Azimuth", "${satellite.azimuth}°")
                                InfoRow("Elevetion", "${satellite.elevation}°")
                            }
                        }
                    }
                }
            }
        }
    }
}
