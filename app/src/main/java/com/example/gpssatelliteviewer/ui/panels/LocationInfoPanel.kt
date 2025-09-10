package com.example.gpssatelliteviewer.ui.panels

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.gpssatelliteviewer.data.GNSSStatusData
import com.example.gpssatelliteviewer.data.ListenerData
import com.example.gpssatelliteviewer.data.NMEAData
import com.example.gpssatelliteviewer.ui.cards.AndroidApiLocation
import com.example.gpssatelliteviewer.ui.cards.LoadingLocationText
import com.example.gpssatelliteviewer.ui.cards.NMEALocationCard
import com.example.gpssatelliteviewer.ui.cards.SatelliteCard
import com.example.gpssatelliteviewer.viewModel.LocationInfoViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.gpssatelliteviewer.utility.NMEAParser

@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationInfoPanel(
    navController: NavController,
    viewModel: /* to change LocationInfoViewModel FakeLocationInfoViewModel*/ LocationInfoViewModel = viewModel()
) {

    val satellites by viewModel.satelliteList.collectAsState()
    val groupedSatellites = satellites.groupBy { it.constellation }

    val locationNMEA by viewModel.locationNMEA.collectAsState()
    val locationAndroidApi by viewModel.locationAndroidApi.collectAsState()

    val message by viewModel.messagePack.collectAsState()

    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location Info Panel") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.Companion
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            item {
                Button(onClick = { navController.navigate("Satellite3DPanel") }) {
                    Text("Widok 3D")
                }
            }
            item {
                if (locationNMEA != null) {
                    NMEALocationCard(locationNMEA)
                } else if (locationAndroidApi != null) {
                    AndroidApiLocation(locationAndroidApi)
                } else {
                    LoadingLocationText()
                }
                Spacer(Modifier.Companion.height(12.dp))
            }

            item {
                SatelliteCard(satellites)
            }


            groupedSatellites.forEach { (constellation, satellitesInGroup) ->

                val expanded = expandedMap.getOrPut(constellation) { false }

                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { expandedMap[constellation] = !expanded },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.Companion
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

                if (expandedMap[constellation] == true) {
                    items(satellitesInGroup) { satellite ->
                        Card(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            modifier = Modifier.Companion
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.Companion.padding(12.dp)) {
                                Text(
                                    "ID: ${satellite.id}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "SNR: ${satellite.snr} dBHz",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Used in Fix: ${satellite.usedInFix}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.R)
@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LocationInfoPanelPreview() {
    val fakeViewModel = FakeLocationInfoViewModel()
    LocationInfoPanel(
        navController = rememberNavController(),
        //viewModel = fakeViewModel
    )
}


// Fake ViewModel only for preview
class FakeLocationInfoViewModel : ViewModel() {

    private val _locationNMEA = MutableStateFlow<NMEAData?>(null)
    val locationNMEA: StateFlow<NMEAData?> = _locationNMEA

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

    private var tmpGGA: NMEAData? = null
    private var tmpRMC: NMEAData? = null
    private var tmpGBS: List<Double>? = null

    init {
        simulateNmeaStream()
    }

    private fun simulateNmeaStream() {

        tmpGGA = NMEAParser.parseGGA(fakeGga, tmpGGA)
        tmpRMC = NMEAParser.parseRMC(fakeRmc, tmpRMC)
        tmpGBS = NMEAParser.parseGBS(fakeGbs, _locationNMEA.value)?.gbsErrors

        _locationNMEA.value = NMEAData(
            time = tmpRMC?.time ?: tmpGGA?.time,
            date = tmpRMC?.date ?: tmpGGA?.date,
            latitude = tmpGGA?.latitude,
            longitude = tmpGGA?.longitude,
            fixQuality = tmpGGA?.fixQuality,
            numSatellites = tmpGGA?.numSatellites,
            hdop = tmpGGA?.hdop,
            altitude = tmpGGA?.altitude,
            geoidHeight = tmpGGA?.geoidHeight,
            mslAltitude = tmpGGA?.mslAltitude,
            speedKnots = tmpRMC?.speedKnots,
            course = tmpRMC?.course,
            magneticVariation = tmpRMC?.magneticVariation,
            gbsErrors = tmpGBS
        )
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
