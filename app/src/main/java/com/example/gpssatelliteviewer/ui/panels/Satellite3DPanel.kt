package com.example.gpssatelliteviewer.ui.panels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gpssatelliteviewer.viewModel.Satellite3DViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.gpssatelliteviewer.rendering3d.Earth3DView
import kotlin.toString

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun Satellite3DPanel(
    navController: NavController,
    viewModel: Satellite3DViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.startGNSSNavigation()
    }

    val navMessage by viewModel.navMessages.collectAsState()
    val gnssCapabilities = viewModel.hasGNSSNavigationMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GNSS navigation raw") }
            )
            Spacer(modifier = Modifier.Companion.height(10.dp))

        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.Companion
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp) // adjust height
                ) {
                    Earth3DView(LocalContext.current)
                }
            }
            item {
                Button(onClick = { navController.navigate("LocationInfoPanel") }) {
                    Text("Satelite 3D Panel")
                }
            }
            item {
                Text(
                    text = gnssCapabilities.toString(),
                    modifier = Modifier.Companion.padding(8.dp)
                )
            }

            if (navMessage != null) {
                item {
                    Text(
                        text = navMessage.toString(),
                        modifier = Modifier.Companion.padding((8.dp))
                    )
                }
            } else {
                item {
                    Text(
                        text = "Waiting for navigation messages...",
                        modifier = Modifier.Companion.padding(8.dp)
                    )
                }
            }
        }
    }
}