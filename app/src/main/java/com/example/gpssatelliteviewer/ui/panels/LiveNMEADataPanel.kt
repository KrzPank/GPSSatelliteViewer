package com.example.gpssatelliteviewer.ui.panels


import android.content.pm.ActivityInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.example.gpssatelliteviewer.ui.cards.DefaultNMEATypeCard
import com.example.gpssatelliteviewer.utils.SetOrientation
import kotlinx.coroutines.flow.compose

private val gsvMessages: List<String> = listOf(
    "GLGSV",
    "GPGSV",
    "GBGSV",
    "GAGSV"
)

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun LiveNMEADataPanel(
    navController: NavController,
    viewModel: GNSSViewModel
) {

    val gga by viewModel.parsedGGA.collectAsState()
    val rmc by viewModel.parsedRMC.collectAsState()
    val gsa by viewModel.parsedGSA.collectAsState()
    val gsv by viewModel.parsedGSV.collectAsState()
    val vtg by viewModel.parsedVTG.collectAsState()

    val nmeaMessageMap by viewModel.nmeaMessageMap.collectAsState()

    val nmeaMessageTypes = nmeaMessageMap.keys.toList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live NMEA messages", style = MaterialTheme.typography.titleLarge) }
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
                var seenGSV = false

                items(
                    items = nmeaMessageTypes,
                ) { type ->
                    if (type.contains("GSV") && !seenGSV) {
                        DefaultNMEATypeCard(
                            "GSV",
                            gga,
                            rmc,
                            gsa,
                            vtg,
                            gsv,
                            nmeaMessageMap[type].toString()
                        )
                        seenGSV = true
                    }
                    else if (type !in gsvMessages){
                        DefaultNMEATypeCard(
                            type,
                            gga,
                            rmc,
                            gsa,
                            vtg,
                            gsv,
                            nmeaMessageMap[type].toString()
                        )
                    }
                }
            }
        }
    }
}