package com.example.gpssatelliteviewer.ui.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
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
import androidx.compose.runtime.setValue
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import com.example.gpssatelliteviewer.data.viewmodel.LocationViewModel
import com.example.gpssatelliteviewer.data.viewmodel.NMEAViewModel
import com.example.gpssatelliteviewer.ui.component.card.GNSSChipsetInfoCard
import com.example.gpssatelliteviewer.ui.component.card.SNRChartCard
import com.example.gpssatelliteviewer.ui.theme.DarkBackground

//*
@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationInfoScreen(
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

    val locationType = when {
        hasLocationNMEA -> "NMEA"
        hasLocationAndroidApi -> "Location Listener"
        else -> "Loading text"
    }
    var selectedLocationType by remember { mutableStateOf(locationType)}

    var showPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxSize()
            .background(DarkBackground),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // --- Location Card ---
        item {
            when (selectedLocationType) {
                "NMEA" -> NMEALocationCard(locationNMEA) { showPicker = true }
                "Location Listener" -> AndroidApiLocationCard(locationAndroidApi) { showPicker = true }
                else -> LoadingLocationTextCard()
            }
        }

        // --- Summary of GPS status ---
        item {
            val hasLocation = when {
                hasLocationNMEA -> true
                hasLocationAndroidApi -> true
                else -> false
            }
            GPSStatusCard(satellites, hasLocation)
        }

        // --- SNR Line Chart ---
        //item {
        //SNRChartCard(
        //    snrHistory,
        //    modifier = Modifier
        //        .weight(1f)
        //        .fillMaxSize()
        //)
        //}
    }
    // Dialog to choose location type
    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Select Location Source") },
            text = {
                Column {
                    Text(
                        "NMEA",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedLocationType = "NMEA"
                                showPicker = false
                            }
                            .padding(12.dp)
                    )
                    Text(
                        "Location Listener",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedLocationType = "Location Listener"
                                showPicker = false
                            }
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {}
        )
    }
}
