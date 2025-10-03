package com.example.gpssatelliteviewer.ui.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gpssatelliteviewer.ui.component.card.ConstellationCard
import com.example.gpssatelliteviewer.ui.component.card.SatelliteInfoCard
import com.example.gpssatelliteviewer.ui.theme.DarkBackground

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun SatelliteScreen(
    navController: NavController,
    gnssViewModel: GNSSViewModel
) {
    val satellites by gnssViewModel.satelliteList.collectAsState()
    val groupedSatellites = satellites.groupBy { it.constellation }

    var dropDownMenuExpanded = remember { mutableStateOf(false) }
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Satellite Info", style = MaterialTheme.typography.headlineMedium) },
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
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .background(DarkBackground),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            groupedSatellites.forEach { (constellation, satellitesInGroup) ->
                val expanded = expandedMap.getOrPut(constellation) { false }

                item {
                    ConstellationCard(
                        constellation = constellation,
                        satellitesCount = satellitesInGroup.size,
                        expanded = expanded,
                        onClick = { expandedMap[constellation] = !expanded }
                    )
                }

                if (expanded) {
                    items(satellitesInGroup) { satellites ->
                        SatelliteInfoCard(satellites)
                    }
                }
            }
        }
    }
}