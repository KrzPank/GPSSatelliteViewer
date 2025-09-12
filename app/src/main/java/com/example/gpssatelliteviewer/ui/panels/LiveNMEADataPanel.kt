package com.example.gpssatelliteviewer.ui.panels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import com.example.gpssatelliteviewer.viewModel.GNSSViewModel

// GOLD NEVER FORGET
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.utils.CoordinateConversion
import com.example.gpssatelliteviewer.utils.InfoRow
import com.example.gpssatelliteviewer.utils.mapFixQuality
import com.example.gpssatelliteviewer.utils.mapFixType

//
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun RawNMEADataPanel(
    navController: NavController,
    viewModel: GNSSViewModel
) {
    val nmeaMessageTypes by viewModel.NMEAMessageType.collectAsState()
    val gga by viewModel.parsedGGA.collectAsState()
    val rmc by viewModel.parsedRMC.collectAsState()
    val gsa by viewModel.parsedGSA.collectAsState()
    val gsv by viewModel.gsvCombined.collectAsState()
    val vtg by viewModel.parsedVTG.collectAsState()
    val nmeaMessage by viewModel.nmeaMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location Info", style = MaterialTheme.typography.titleLarge) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Button(onClick = { navController.navigate("LocationInfoPanel") }) {
                Text("Panel lokalizacji")
            }

            LazyColumn(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                items(nmeaMessageTypes) { type ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = type,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            when {
                                type.contains("GGA", ignoreCase = true) && gga != null -> {
                                    InfoRow("Raw message", gga!!.message)
                                    InfoRow("Time (UTC)", gga!!.time)
                                    InfoRow("Latitude", CoordinateConversion.geodeticToDMS(gga!!.latitude, gga!!.latDirection))
                                    InfoRow("Longitude", CoordinateConversion.geodeticToDMS(gga!!.longitude, gga!!.lonDirection))
                                    InfoRow("Fix Quality", mapFixQuality(gga!!.fixQuality))
                                    InfoRow("Satellites", gga!!.numSatellites.toString())
                                    InfoRow("HDOP", gga!!.horizontalDilution.toString())
                                    InfoRow("Altitude", "${gga!!.altitude} ${gga!!.altitudeUnits}")
                                    InfoRow("Geoid Separation", "${gga!!.geoidSeparation} ${gga!!.geoidSeparationUnits}")
                                }

                                /* Something is wrong in RMC not all info is being passed */
                                type.contains("RMC", ignoreCase = true) && rmc != null -> {
                                    InfoRow("Raw message", rmc!!.message)
                                    InfoRow("Time (UTC)", rmc!!.time)
                                    InfoRow("Date", rmc!!.date)
                                    InfoRow("Latitude", CoordinateConversion.geodeticToDMS(rmc!!.latitude, rmc!!.latDirection))
                                    InfoRow("Longitude", CoordinateConversion.geodeticToDMS(rmc!!.longitude, rmc!!.lonDirection))
                                    InfoRow("Speed (knots)", rmc!!.speedOverGround.toString())
                                    InfoRow("Course", rmc!!.courseOverGround.toString())
                                    InfoRow("Magnetic Variation", rmc!!.magneticVariation?.toString() ?: "-")
                                }

                                type.contains("GSA", ignoreCase = true) && gsa != null -> {
                                    InfoRow("Raw message", gsa!!.message)
                                    InfoRow("Mode", gsa!!.mode.toString())
                                    InfoRow("Fix Type", mapFixType(gsa!!.fixType))
                                    InfoRow("PDOP", gsa!!.pdop.toString())
                                    InfoRow("HDOP", gsa!!.hdop.toString())
                                    InfoRow("VDOP", gsa!!.vdop.toString())
                                    InfoRow("Satellites IDs", gsa!!.satelliteIds.joinToString(", "))
                                }

                                type.contains("VTG", ignoreCase = true) && vtg != null -> {
                                    InfoRow("Raw message", vtg!!.message)
                                    InfoRow("Course True", vtg!!.courseTrue.toString())
                                    InfoRow("Course Magnetic", vtg!!.courseMagnetic?.toString() ?: "N/A")
                                    InfoRow("Speed Knots", vtg!!.speedKnots.toString())
                                    InfoRow("Speed Km/h", vtg!!.speedKmph.toString())
                                }
/*
                                type.contains("GSV", ignoreCase = true) && gsv.isNotEmpty() -> {
                                    InfoRow("Satellites in view", gsv.size.toString())
                                    gsv.forEachIndexed { index, sat ->
                                        InfoRow(
                                            "PRN ${index + 1}",
                                            "Elevation: ${sat.elevation}, Azimuth: ${sat.azimuth}, SNR: ${sat.snr}"
                                        )
                                    }
                                }
 */
                                else -> {
                                    InfoRow("Raw message", nmeaMessage.toString())
                                    InfoRow("Info", "No data available")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}