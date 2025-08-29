package com.example.gpssatelliteviewer.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf


import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.gpssatelliteviewer.data.GNSSData
import com.example.gpssatelliteviewer.data.ListenerData
import com.example.gpssatelliteviewer.data.NMEAData
import com.example.gpssatelliteviewer.ui.cards.AndroidApiLocation
import com.example.gpssatelliteviewer.ui.cards.NMEALocationCard
import com.example.gpssatelliteviewer.ui.cards.SatelliteCard
import com.example.gpssatelliteviewer.ui.components.InfoRow
import com.example.gpssatelliteviewer.ui.components.LoadingLocationText
import com.example.gpssatelliteviewer.utility.approximateLocationAccuracy
import com.example.gpssatelliteviewer.utility.parseGBS
import com.example.gpssatelliteviewer.utility.parseGGA
import com.example.gpssatelliteviewer.utility.parseRMC
import com.example.gpssatelliteviewer.viewModel.SatelliteViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.text.format


@RequiresApi(Build.VERSION_CODES.R)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatellitePanel(
    navController: NavController,
    viewModel: /* to change SatelliteViewModel FakeSatelliteViewModel*/ SatelliteViewModel = viewModel()
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
                title = { Text("GNSS Monitor") }
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
                if (locationNMEA != null) {
                    NMEALocationCard(locationNMEA)
                } else if (locationAndroidApi != null) {
                    AndroidApiLocation(locationAndroidApi)
                } else {
                    LoadingLocationText()
                }
                Spacer(Modifier.height(12.dp))
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.Companion
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



/*
@RequiresApi(Build.VERSION_CODES.R)
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


// Fake ViewModel tylko dla preview
class FakeSatelliteViewModel : ViewModel() {

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


    private val fakeGga = "\$GPGGA,172814.0,3723.46587704,N,12202.26957864,W,2,6,1.2,18.893,M,25.669,M,2.00031*4F"
    private val fakeRmc = "\$GNRMC,060512.00,A,3150.788156,N,11711.922383,E,0.0,,311019,,,A,V*1B"
    private val fakeGbs = "\$GPGBS,015509.00,-0.031,-0.186,0.219,19,0.000,-0.354,6.972*4D"

    /*
    private val fakeGga = "\$GPGGA,123519,3746.483,N,12225.150,W,2,09,0.9,15.3,M,-31.2,M,,*5A"
    private val fakeRmc = "\$GPRMC,123519,A,3746.483,N,12225.150,W,5.7,187.3,280825,3.1,W*6A"
    private val fakeGbs = "\$GPGBS,123519,1.2,0.8,2.5,04,,,*5C"
    */

    init {
        simulateNmeaStream()
    }

    private fun simulateNmeaStream() {
            _locationNMEA.value = parseGGA(fakeGga, _locationNMEA.value)
            _locationNMEA.value = parseRMC(fakeRmc, _locationNMEA.value)
            _locationNMEA.value = parseGBS(fakeGbs, _locationNMEA.value)
    }

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