package com.example.gpssatelliteviewer.ui.screen


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.navigation.NavController

// GOLD NEVER FORGET
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.ui.component.card.DefaultNMEATypeCard
import com.example.gpssatelliteviewer.data.viewmodel.NMEAViewModel

private val gsvMessages: List<String> = listOf(
    "GLGSV",
    "GPGSV",
    "GBGSV",
    "GAGSV"
)

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun LiveNMEADataScreen(
    navController: NavController,
    viewModel: NMEAViewModel
) {
    val gga by viewModel.parsedGGA.collectAsState()
    val rmc by viewModel.parsedRMC.collectAsState()
    val gsa by viewModel.parsedGSA.collectAsState()
    val gsv by viewModel.parsedGSV.collectAsState()
    val vtg by viewModel.parsedVTG.collectAsState()

    val nmeaMessageMap by viewModel.nmeaMessageMap.collectAsState()

    val nmeaMessageTypes = nmeaMessageMap.keys.toList()

    var dropDownMenuExpanded = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Live NMEA messages", style = MaterialTheme.typography.titleLarge)
                },
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
                            DropdownMenuItem(
                                text = { Text("Location Info") },
                                onClick = {
                                    dropDownMenuExpanded.value = false
                                    navController.navigate("LocationInfoScreen")
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
                .fillMaxSize()
        ) {
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