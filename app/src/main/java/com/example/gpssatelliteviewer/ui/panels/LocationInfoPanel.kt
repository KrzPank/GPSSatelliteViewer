package com.example.gpssatelliteviewer.ui.panels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import com.example.gpssatelliteviewer.ui.cards.AndroidApiLocation
import com.example.gpssatelliteviewer.ui.cards.LoadingLocationText
import com.example.gpssatelliteviewer.ui.cards.NMEALocationCard
import com.example.gpssatelliteviewer.ui.cards.SatelliteCard


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.example.gpssatelliteviewer.utils.InfoRow
import com.example.gpssatelliteviewer.viewModel.GNSSViewModel

//*
@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationInfoPanel(
    navController: NavController,
    viewModel: GNSSViewModel
) {
    val satellites by viewModel.satelliteList.collectAsState()
    val groupedSatellites = satellites.groupBy { it.constellation }

    val locationNMEA by viewModel.locationNMEA.collectAsState()
    val locationAndroidApi by viewModel.locationAndroidApi.collectAsState()

    val hasLocationNMEA by viewModel.hasLocationNMEA.collectAsState()
    val hasLocationAndroidApi by viewModel.hasLocationAndroidApi.collectAsState()

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
                            /*
                            * NMEA what message
                            * RAW message
                            * thing we see from them...
                            */
                            DropdownMenuItem(
                                text = { Text("NMEA messaegs") },
                                onClick = {
                                    dropDownMenuExpanded.value = false
                                    navController.navigate("RawNMEADataPanel")
                                }
                            )
                            /*
                            *** TO DO ADD MORE PANELS
                            *
                            * ONE WILL BE NMEA MESSAGES DISPLAY
                            *
                            *
                            */

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
                    hasLocationAndroidApi -> AndroidApiLocation(locationAndroidApi)
                    else -> LoadingLocationText()
                }
            }

            // --- Summary of satellites ---
            item {
                SatelliteCard(satellites)
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
                                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
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
                                InfoRow("ID", satellite.prn.toString())
                                InfoRow("SNR", "${satellite.snr} dBHz")
                                InfoRow("Used in Fix", satellite.usedInFix.toString())
                            }
                        }
                    }
                }
            }

        }
    }
}

/* Fake viewModel for preview
@RequiresApi(Build.VERSION_CODES.R)
@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LocationInfoPanelPreview() {
    val fakeViewModel: FakeLocationInfoViewModel = viewModel()

}

class FakeLocationInfoViewModel : ViewModel() {

    private val _locationNMEA = MutableStateFlow<NMEALocationData>(NMEALocationData())
    val locationNMEA: StateFlow<NMEALocationData> = _locationNMEA

    private val _hasLocationAndroidApi = MutableStateFlow<Boolean>(false)
    val hasLocationAndroidApi: StateFlow<Boolean> = _hasLocationAndroidApi

    private val _hasLocationNMEA = MutableStateFlow<Boolean>(false)
    val hasLocationNMEA: StateFlow<Boolean> = _hasLocationNMEA

   // /*
        private val _locationAndroidApi = MutableStateFlow<ListenerData?>(
       ListenerData(
           time = "183654",
           latitude = 12.2297,
           longitude = 21.0122,
           altitude = 10.0,
           latHemisphere = "N",
           longHemisphere = "E"
       )
   )
    //*/

    val locationAndroidApi: StateFlow<ListenerData?> = _locationAndroidApi

    private val _messagePack = MutableStateFlow<String?>("Testing")
    val messagePack: StateFlow<String?> = _messagePack

    private val fakeGga = "\$GPGGA,172814.0,3723.46587704,N,12202.26957864,W,2,6,1.2,18.893,M,-25.669,M,2.0 0031*4F"
    private val fakeRmc = "\$GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A"
    private val fakeGbs = "\$GPGBS,015509.00,-0.031,-0.186,0.219,19,0.000,-0.354,6.972*4D"

    private var tmpGGA: GGA? = null
    private var tmpRMC: RMC? = null

    init {
        simulateNmeaStream()
    }

    private fun simulateNmeaStream() {
        tmpGGA = NMEAParser.parseGGA(fakeGga)
        tmpRMC = NMEAParser.parseRMC(fakeRmc)

        val combined = NMEALocationData(
            time = tmpRMC?.time ?: tmpGGA?.time.orEmpty(),
            date = tmpRMC?.date ?: tmpGGA?.time.orEmpty(),
            latitude = tmpGGA?.latitude?.takeIf { it != 0.0 } ?: (tmpRMC?.latitude ?: 0.0),
            longitude = tmpGGA?.longitude?.takeIf { it != 0.0 } ?: (tmpRMC?.longitude ?: 0.0),
            fixQuality = tmpGGA?.fixQuality ?: 0,
            numSatellites = tmpGGA?.numSatellites ?: 0,
            hdop = tmpGGA?.horizontalDilution ?: 0.0,
            altitude = tmpGGA?.altitude ?: 0.0,
            geoidHeight = tmpGGA?.geoidSeparation ?: 0.0,
            mslAltitude = tmpGGA?.let { gga ->
                gga.altitude + (gga.geoidSeparation ?: 0.0)
            } ?: 0.0,
            speedKnots = tmpRMC?.speedOverGround ?: 0.0,
            course = tmpRMC?.courseOverGround ?: 0.0,
            magneticVariation = tmpRMC?.magneticVariation ?: 0.0
        )
        _locationNMEA.value = combined
    }

    private val _satelliteList = MutableStateFlow(
        listOf(
            GNSSStatusData("GPS", 12, 35.5f, true, 120.0f, 45.0f),
            GNSSStatusData("GPS", 14, 32.1f, true, 135.0f, 40.5f),
            GNSSStatusData("GPS", 18, 28.4f, false, 150.0f, 35.0f),
            GNSSStatusData("GLONASS", 3, 28.7f, false, 100.0f, 42.0f),
            GNSSStatusData("GLONASS", 7, 30.0f, true, 110.0f, 43.5f),
            GNSSStatusData("Galileo", 5, 42.0f, true, 130.0f, 47.0f),
            GNSSStatusData("Galileo", 9, 37.5f, false, 140.0f, 41.0f),
            GNSSStatusData("Galileo", 12, 33.2f, true, 145.0f, 44.0f),
            GNSSStatusData("BeiDou", 8, 30.2f, false, 125.0f, 38.5f),
            GNSSStatusData("BeiDou", 10, 27.9f, true, 128.0f, 39.0f),
            GNSSStatusData("QZSS", 193, 25.4f, true, 160.0f, 30.0f),
            GNSSStatusData("QZSS", 195, 22.7f, false, 165.0f, 28.0f),
            GNSSStatusData("SBAS", 120, 20.0f, false, 170.0f, 25.0f),
            GNSSStatusData("SBAS", 123, 21.5f, true, 175.0f, 26.5f),
            GNSSStatusData("IRNSS", 11, 33.8f, true, 115.0f, 41.5f),
            GNSSStatusData("IRNSS", 14, 29.9f, false, 120.0f, 39.0f),
            GNSSStatusData("GPS", 21, 31.2f, true, 137.0f, 43.0f),
            GNSSStatusData("GLONASS", 11, 26.5f, false, 112.0f, 37.0f),
            GNSSStatusData("Galileo", 15, 36.1f, true, 142.0f, 42.5f)
        )
    )

    val satelliteList: StateFlow<List<GNSSStatusData>> = _satelliteList
}


 */