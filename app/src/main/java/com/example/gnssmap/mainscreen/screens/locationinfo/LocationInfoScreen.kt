package com.example.gnssmap.mainscreen.screens.locationinfo

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.gnssmap.data.viewmodel.GNSSViewModel
import com.example.gnssmap.data.viewmodel.LocationViewModel
import com.example.gnssmap.data.viewmodel.NMEAViewModel
import com.example.gnssmap.utils.EmptyStateCard
import kotlinx.coroutines.delay
import java.sql.Date
import java.util.Locale

const val IS_LOCATION_ENABLED_TIMER = 3000L  // 1 sec wait

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationInfoScreen(
    gnssStatusViewModel: GNSSViewModel,
    nmeaViewModel: NMEAViewModel,
    locationViewModel: LocationViewModel
) {
    val satellites by gnssStatusViewModel.satelliteList.collectAsState()

    val locationNMEA by nmeaViewModel.locationNMEA.collectAsState()
    val hasLocationNMEA by nmeaViewModel.hasLocationNMEA.collectAsState()

    val locationAndroidApi by locationViewModel.locationAndroidApi.collectAsState()
    val hasLocationAndroidApi by locationViewModel.hasLocationAndroidApi.collectAsState()

    val locationType = when {
        hasLocationNMEA -> "NMEA"
        hasLocationAndroidApi -> "Location Listener"
        else -> "Waiting for location..."
    }

    var selectedLocationType by remember { mutableStateOf(locationType) }
    var showPicker by remember { mutableStateOf(false) }
    var userHasSelectedType by remember { mutableStateOf(false) }

    LaunchedEffect(locationType) {
        if (!userHasSelectedType) {
            selectedLocationType = locationType
        }
    }

    val isLocationEnabled by locationViewModel.isLocationEnabled.collectAsState()
    LaunchedEffect(Unit) {
        while (true) {
            locationViewModel.checkLocationEnabled()
            delay(IS_LOCATION_ENABLED_TIMER)
        }
    }

    var currentSystemTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            val date = Date(System.currentTimeMillis())
            currentSystemTime = sdf.format(date)
            delay(330L) // wait 330ms. More and updates seem not consistent?
        }
    }

    LazyColumn(
        modifier = Modifier.Companion
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // --- Location Card ---
        item {
            when (selectedLocationType) {
                "NMEA" -> NMEALocationCard(locationNMEA, currentSystemTime) {
                    showPicker = true
                    userHasSelectedType = true
                }

                "Location Listener" -> AndroidApiLocationCard(locationAndroidApi, currentSystemTime) {
                    showPicker = true
                    userHasSelectedType = true
                }

                "Waiting for location..." -> EmptyStateCard(
                    message = "No location received",
                    icon = Icons.Default.LocationOn
                )
            }
        }

        // --- Summary of GPS status ---
        item {
            val hasLocation = when {
                hasLocationNMEA -> true
                hasLocationAndroidApi -> true
                else -> false
            }
            GNSSStatusCard(
                satellites = satellites,
                hasLocation = hasLocation,
                isLocationEnabled = isLocationEnabled
            )
        }
    }

    // Dialog to choose location type
    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Select Location Source") },
            text = {
                Column {
                    Text(
                        "NMEA Location",
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .clickable {
                                selectedLocationType = "NMEA"
                                showPicker = false
                            }
                            .padding(12.dp)
                    )
                    Text(
                        "Android Api Location",
                        modifier = Modifier.Companion
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
