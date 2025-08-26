package com.example.gpssatelliteviewer.view

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.gpssatelliteviewer.data.GNSSData
import com.example.gpssatelliteviewer.data.NMEAData
import com.example.gpssatelliteviewer.utility.InfoRow
import com.example.gpssatelliteviewer.utility.LoadingLocationText
import com.example.gpssatelliteviewer.utility.formatNmeaTime
import com.example.gpssatelliteviewer.utility.mapFixQuality
import com.example.gpssatelliteviewer.viewModel.SatelliteViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/*
@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SatellitePanelPreview() {
    val fakeViewModel = FakeSatelliteViewModel()
    SatellitePanel(
        navController = rememberNavController(),
        viewModel = fakeViewModel
    )
}
*/


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatellitePanel(
    navController: NavController,
    viewModel: /* to change SatelliteViewModel FakeSatelliteViewModel*/ SatelliteViewModel = viewModel()
) {
    val satellites by viewModel.satelliteList.collectAsState()
    val groupedSatellites = satellites.groupBy { it.constellation }

    val location by viewModel.location.collectAsState()

    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GNSS Monitor") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {

            // Location card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        if (location == null) {
                            // Waiting for location
                            LoadingLocationText()
                        } else {
                            // Display NMEAData
                            val nmea = location!!
                            Text("Lokalizacja", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))

                            InfoRow(label = "Czas (UTC)", value = formatNmeaTime(nmea.time))
                            InfoRow(label = "Szerokość geograficzna", value = "%.6f°".format(nmea.latitude))
                            InfoRow(label = "Długość geograficzna", value = "%.6f°".format(nmea.longitude))
                            InfoRow(label = "Wysokość", value = "%.1f m".format(nmea.altitude))
                            InfoRow(label = "Wysokość geoidy nad elipsoidą", value = "%.1f m".format(nmea.heightOfGeoid))
                            InfoRow(label = "Dokładność pozioma (HDOP)", value = "%.1f".format(nmea.horizontalDilution))
                            InfoRow(label = "Liczba używanych satelitów", value = nmea.numSatellites.toString())
                            InfoRow(label = "Jakość fixa", value = mapFixQuality(nmea.fixQuality))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            groupedSatellites.forEach { (constellation, satellitesInGroup) ->

                val expanded = expandedMap.getOrPut(constellation) { false }

                // Header dla konstelacji
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { expandedMap[constellation] = !expanded },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$constellation (${satellitesInGroup.size})",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                // expanded panel
                if (expandedMap[constellation] == true) {
                    items(satellitesInGroup) { satellite ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("ID: ${satellite.id}", style = MaterialTheme.typography.bodyMedium)
                                Text("SNR: ${satellite.snr} dBHz", style = MaterialTheme.typography.bodyMedium)
                                Text("Used in Fix: ${satellite.usedInFix}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}




// Fake ViewModel tylko dla preview
class FakeSatelliteViewModel : ViewModel() {
    private val _location = MutableStateFlow<NMEAData?>(
        NMEAData(
            time = "123519",            // UTC time
            latitude = 52.2297,         // example latitude
            longitude = 21.0122,        // example longitude
            fixQuality = 1,             // GPS fix
            numSatellites = 8,          // number of satellites
            horizontalDilution = 0.9f,  // HDOP
            altitude = 100.0,           // altitude in meters
            heightOfGeoid = 46.9        // geoid height
        )
    )

    val location: StateFlow<NMEAData?> = _location

    private val _satelliteList = MutableStateFlow(
        listOf(
            GNSSData("GPS", 12, 35.5f, true),
            GNSSData("GPS", 14, 32.1f, true),
            GNSSData("GPS", 18, 28.4f, false),
            GNSSData("GLONASS", 3, 28.7f, false),
            GNSSData("GLONASS", 7, 30.0f, true),
            GNSSData("Galileo", 5, 42.0f, true),
            GNSSData("Galileo", 9, 37.5f, false),
            GNSSData("Galileo", 12, 33.2f, true),
            GNSSData("BeiDou", 8, 30.2f, false),
            GNSSData("BeiDou", 10, 27.9f, true),
            GNSSData("QZSS", 193, 25.4f, true),
            GNSSData("QZSS", 195, 22.7f, false),
            GNSSData("SBAS", 120, 20.0f, false),
            GNSSData("SBAS", 123, 21.5f, true),
            GNSSData("IRNSS", 11, 33.8f, true),
            GNSSData("IRNSS", 14, 29.9f, false),
            GNSSData("GPS", 21, 31.2f, true),
            GNSSData("GLONASS", 11, 26.5f, false),
            GNSSData("Galileo", 15, 36.1f, true)
        )
    )

    val satelliteList: StateFlow<List<GNSSData>> = _satelliteList
}
