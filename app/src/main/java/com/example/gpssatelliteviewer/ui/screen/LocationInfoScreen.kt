package com.example.gpssatelliteviewer.ui.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gpssatelliteviewer.ui.component.card.AndroidApiLocationCard
import com.example.gpssatelliteviewer.ui.component.card.LoadingLocationTextCard
import com.example.gpssatelliteviewer.ui.component.card.NMEALocationCard
import com.example.gpssatelliteviewer.ui.component.card.GPSStatusCard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import com.example.gpssatelliteviewer.data.viewmodel.LocationViewModel
import com.example.gpssatelliteviewer.data.viewmodel.NMEAViewModel
import com.example.gpssatelliteviewer.ui.component.card.SNRChartCard
import com.example.gpssatelliteviewer.ui.theme.DarkBackground

//*
@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationInfoScreen(
    navController: NavController,
    gnssStatusViewModel: GNSSViewModel,
    nmeaViewModel: NMEAViewModel,
    locationViewModel: LocationViewModel
) {
    val satellites by gnssStatusViewModel.satelliteList.collectAsState()
    val snrHistory by gnssStatusViewModel.snrHistory.collectAsState()

    val locationNMEA by nmeaViewModel.locationNMEA.collectAsState()
    val locationAndroidApi by locationViewModel.locationAndroidApi.collectAsState()

    val hasLocationNMEA by nmeaViewModel.hasLocationNMEA.collectAsState()
    val hasLocationAndroidApi by locationViewModel.hasLocationAndroidApi.collectAsState()

    var dropDownMenuExpanded = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location Info", style = MaterialTheme.typography.headlineMedium) },
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
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .fillMaxWidth()
                .background(DarkBackground),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // --- Location Card ---
            //item {
                when {
                    hasLocationNMEA -> NMEALocationCard(locationNMEA)
                    hasLocationAndroidApi -> AndroidApiLocationCard(locationAndroidApi)
                    else -> LoadingLocationTextCard()
                }
            //}

            // --- Summary of GPS status ---
            //item {
                val hasLocation = when {
                    hasLocationNMEA -> true
                    hasLocationAndroidApi -> true
                    else -> false
                }
                GPSStatusCard(satellites, hasLocation)
            //}

            // --- SNR Line Chart ---
            //item {
            SNRChartCard(
                snrHistory,
                timeStamp = locationNMEA.time,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            )
            //}
        }
    }
}
