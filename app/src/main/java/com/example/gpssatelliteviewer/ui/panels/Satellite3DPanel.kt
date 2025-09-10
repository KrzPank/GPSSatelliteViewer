package com.example.gpssatelliteviewer.ui.panels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.gpssatelliteviewer.rendering3d.Earth3DScene
import com.example.gpssatelliteviewer.viewModel.LocationInfoViewModel


//import com.example.gpssatelliteviewer.viewModel.Satellite3DViewModel

@RequiresApi(Build.VERSION_CODES.R)
class Satellite3DPanel(viewModel: LocationInfoViewModel) {

    val satelliteList = viewModel.satelliteList
}

//*
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun Satellite3DPanel(
    navController: NavController,
    viewModel: LocationInfoViewModel = viewModel()
) {
    val navMessage by viewModel.navMessages.collectAsState()
    val gnssCapabilities = viewModel.hasGNSSNavigationMessage.collectAsState()
    val context = LocalContext.current

    val satelliteList by viewModel.satelliteList.collectAsState()
    val locationNMEA by viewModel.locationNMEA.collectAsState()

    val userLocation: Pair<String?, String?> = Pair(locationNMEA?.latitude, locationNMEA?.longitude)

    Scaffold { innerPadding ->

        Column ( modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        ){
            Button(onClick = { navController.navigate("LocationInfoPanel") }) {
                Text("Panel lokalizacji")
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Earth3DScene(satelliteList)
            }

            /* *** INFORMATION CHECK WORKS ***
            Text(
                text = gnssCapabilities.toString(),
                modifier = Modifier.Companion.padding(8.dp)
            )
            if (navMessage != null) {
                Text(
                    text = navMessage.toString(),
                    modifier = Modifier.Companion.padding((8.dp))
                )
            } else {
                Text(
                    text = "Waiting for navigation messages...",
                    modifier = Modifier.Companion.padding(8.dp)
                )
            }
            */
        }
    }
}

//*/