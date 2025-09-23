package com.example.gpssatelliteviewer

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gpssatelliteviewer.ui.panels.LiveNMEADataPanel

import com.example.gpssatelliteviewer.ui.panels.LocationDeny
import com.example.gpssatelliteviewer.ui.panels.LocationInfoPanel
import com.example.gpssatelliteviewer.ui.panels.Satellite3DPanel
import com.example.gpssatelliteviewer.viewModel.GNSSViewModel
import com.example.gpssatelliteviewer.viewModel.LocationListenerViewModel
import com.example.gpssatelliteviewer.viewModel.NMEAViewModel


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current

            // null = not checked yet, true/false = actual state
            var hasPermission by remember { mutableStateOf<Boolean?>(null) }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { granted -> hasPermission = granted }
            )

            LaunchedEffect(Unit) {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                hasPermission = granted

                if (!granted) {
                    launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            MaterialTheme {
                Surface {
                    when (hasPermission) {
                        null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        true -> AppNavigation(hasPermission = true, permissionLauncher = launcher)
                        false -> AppNavigation(hasPermission = false, permissionLauncher = launcher)
                    }
                }
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun AppNavigation(
    hasPermission: Boolean, 
    permissionLauncher: androidx.activity.compose.ManagedActivityResultLauncher<String, Boolean>
) {
    val navController = rememberNavController()

    if (hasPermission) {
        val context = LocalContext.current
        val app = context.applicationContext as Application

        val gnssViewModel: GNSSViewModel = viewModel(
            factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )
        val nmeaViewModel: NMEAViewModel = viewModel(
            factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )
        val locationListenerViewModel: LocationListenerViewModel = viewModel(
            factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )

        gnssViewModel.startLocationInfo()
        nmeaViewModel.startNMEAInfo()
        locationListenerViewModel.startLocationListenerInfo()

        NavHost(
            navController = navController,
            startDestination = "LocationInfoPanel"
        ) {
            composable("LocationInfoPanel") {
                LocationInfoPanel(navController, gnssViewModel, nmeaViewModel, locationListenerViewModel)
            }
            composable("Satellite3DPanel") {
                val locationNMEA by nmeaViewModel.locationNMEA.collectAsState()
                Satellite3DPanel(navController, gnssViewModel, locationNMEA)
            }
            composable("LiveNMEADataPanel") {
                LiveNMEADataPanel(navController, nmeaViewModel)
            }
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = "LocationDeny"
        ) {
            composable("LocationDeny") {
                LocationDeny(
                    navController = navController,
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                )
            }
        }
    }
}
