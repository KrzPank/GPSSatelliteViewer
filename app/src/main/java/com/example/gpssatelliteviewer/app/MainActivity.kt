package com.example.gpssatelliteviewer.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import com.example.gpssatelliteviewer.app.theme.GPSSatelliteViewerTheme
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
import com.example.gpssatelliteviewer.data.viewmodel.GNSSViewModel
import com.example.gpssatelliteviewer.data.viewmodel.LocationViewModel
import com.example.gpssatelliteviewer.data.viewmodel.NMEAViewModel
import com.example.gpssatelliteviewer.locationdeny.LocationDenyScreen
import com.example.gpssatelliteviewer.mainscreen.MainScreen
import com.example.gpssatelliteviewer.scene3d.ui.Satellite3DScreen
import com.example.gpssatelliteviewer.statisticscreen.MainStatisticsScreen
import com.example.gpssatelliteviewer.utils.SetupDarkSystemUI

class MainActivity : ComponentActivity() {
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

            GPSSatelliteViewerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (hasPermission) {
                        null -> {
                            Box(
                                modifier = Modifier.Companion.fillMaxSize(),
                                contentAlignment = Alignment.Companion.Center
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


@Composable
fun AppNavigation(
    hasPermission: Boolean,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    SetupDarkSystemUI()
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
        val locationViewModel: LocationViewModel = viewModel(
            factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )

        gnssViewModel.startGNSSInfo()
        gnssViewModel.loadGNSSHardwareInfo()
        nmeaViewModel.startNMEAInfo()
        locationViewModel.startLocationListenerInfo()

        NavHost(
            navController = navController,
            startDestination = "MainScreen"
        ) {
            composable("MainScreen") {
                MainScreen(
                    navController = navController,
                    gnssViewModel =  gnssViewModel,
                    nmeaViewModel = nmeaViewModel,
                    locationViewModel = locationViewModel
                )
            }
            composable("Satellite3DScreen") {
                val locationNMEA by nmeaViewModel.locationNMEA.collectAsState()
                Satellite3DScreen(
                    navController = navController,
                    gnssViewModel = gnssViewModel,
                    locationNMEA = locationNMEA,
                    locationViewModel = locationViewModel
                )
            }
            composable("MainStatisticsScreen") {
                val nmeaMessageStatistics by nmeaViewModel.messageStatistics.collectAsState()
                MainStatisticsScreen(
                    navController = navController,
                    gnssViewModel = gnssViewModel,
                    nmeaMessageStatistics = nmeaMessageStatistics
                )
            }
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = "LocationDenyScreen"
        ) {
            composable("LocationDenyScreen") {
                LocationDenyScreen(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                )
            }
        }
    }
}